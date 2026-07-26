# 压测与优化验证手册（IDEA + JMeter 版）

本手册用于把简历里的两条性能指标**真实跑出来并坐实**：

- 「Redis 缓存使组织架构树接口 P95 由 524 ms 降至 8 ms、命中率 98.8%」
- 「通过 EXPLAIN 重建复合索引优化查询计划」

> ⚠️ **最重要的一句话**：简历里的压测数字必须来自你**自己机器上真实跑出的值**，不能编。
> ✅ 本轮（2026-07-23 第六轮，含 Warmup 预热）已实测回填：P95 **524ms → 8ms（↓98.5%）**、平均 452ms → 5ms、吞吐量 63 → 1237 QPS、命中率 **98.8%**。详见同目录 `RESULTS.md`。
> 造假一次，前面所有努力归零——这一步的意义就是让数字变成可验证的事实。

---

## 一、环境准备

- **后端在 IntelliJ IDEA 中启动**（见第二节），不再用命令行 `mvn spring-boot:run`
- MySQL 8 / Redis 5.0.10 已运行，已执行 `db/schema.sql` + `db/data.sql`
- **额外执行 `db/seed-data.sql`** 灌入 2000 部门 + 6000 员工中等数据量
  （脚本会先 TRUNCATE 清掉旧数据，重跑可覆盖；压测不需要 data.sql 的 10 条基础部门）
  > ⚠️ **数据量是缓存差异的前提，但要取平衡点**：太小（原 10 行）MySQL 亚毫秒返回，
  > 加不加缓存没区别（你第一轮 P95 全是 2~4ms）；太大（原 1 万）无缓存查询慢到十几秒，
  > 又会被 HikariCP 10 连接池排队淹没（你第三轮无缓存/有缓存 P95 都是 31s）。
  > **2000 行** 是让"无缓存几十~几百 ms、有缓存十几 ms"合理拉开差距的甜点。
- **JMeter** 已下载解压（计划文件 `org-tree-bench.jmx` 在同目录）
- 压测请求走 `GET http://localhost:8080/api/org/tree`
  （**轻量端点**：返回 `List<Dept>`，由 `@Cacheable` 缓存；靠 `seed-data.sql` 灌入的
  2000 部门中等数据，让无缓存查询明显变慢、又不至于把连接池打爆，缓存优势得以体现）
  > 之前的 `tree-stats`（递归 CTE + 员工统计，大对象 JSON 序列化）实测缓存反而更慢
  > （P95 31s vs 无缓存 4s、错误率 9%），已弃用；若想讲「重查询优化」可后续加 Caffeine 本地缓存再测。

---

## 二、在 IDEA 中启动后端（两种场景）

### 场景 A：关缓存（模拟「优化前」）

1. 打开主类 `MtApplication`，点顶部 **Run / Debug Configurations → Edit Configurations**
2. 选中该配置，点复制图标，命名为 **`MtApplication-NoCache`**
3. 在 **Environment variables** 里添加：
   ```
   SPRING_CACHE_TYPE=none
   ```
4. 用 `MtApplication-NoCache` 这个配置 **Run** 启动

> 用环境变量而非改 `application.yml`，好处是不污染代码、切换场景只换配置即可。

> ⚠️ **已修复一个隐藏 bug**：之前这个配置其实没真正关掉缓存——因为我们手动定义了
> `RedisCacheManager` bean，而 `SPRING_CACHE_TYPE=none` 只能让 Spring Boot 的**自动配置**退场，
> 手写的 bean 仍会被注册，导致 `@Cacheable` 依旧往 Redis 写。现已在 `RedisConfig.cacheManager`
> 上加 `@ConditionalOnProperty(... type=redis ...)`：当 `type=none` 时该 bean 不创建，
> Spring 自动回退到 `NoOpCacheManager`，缓存才真正关闭。

**场景 A 启动后务必验证缓存已关闭**（否则压测对照失真）：
```bash
# 1) 启动后端前先清空 Redis，清掉场景 B 残留的 key
redis-cli flushall
# 2) 跑完 JMeter 后，检查 Redis 里是否还有 key
redis-cli dbsize        # 场景 A 正常应返回 0；若 >0 说明缓存仍在写入，需检查配置
```

### 场景 B：开缓存（模拟「优化后」）

1. 用默认的 `MtApplication` 配置 **Run** 启动（缓存生效）
   > 跑场景 B 前，先 `redis-cli flushall` 清空 Redis（清掉场景 A 残留，并让命中率统计从冷启动开始）。
2. **无需手动预热**：jmx 里的 **Warmup 线程组**会在正式压测前自动打满预热请求并写入缓存，
   你只需正常跑 JMeter 即可。唯一要做的就是上面第 1 步的 `flushall`。

---

## 三、用 JMeter 施压（替代 ab）

压测计划 `org-tree-bench.jmx` 已配好，含 **两个顺序执行的 Thread Group**（已设 `serialize_threadgroups=true`）：

- **Warmup 组**（自动）：30 线程 × 15 循环，请求 label 为 `WARMUP /api/org/tree`，
  先于正式组跑完，**专门用来触发 JVM 预热（类加载 / JIT 编译 / 连接池初始化）和首次缓存写入**，
  其样本与正式指标 label 不同，**不会污染**正式行。
- **OrgTree Users 组**（正式）：30 线程 × 50 循环 = **1500 请求**，label 为 `GET /api/org/tree`，
  `ramp_time = 1` 秒，带 `X-Tenant-Id: 1` 头，输出 Summary Report + Aggregate Report。

> ⚠️ **关键纪律（防上次的 3000 样本叠加）**：GUI 每次点 ▶ 运行前，
> **必须先点工具栏的扫帚图标「Clear All」清空上一次的报告表**，否则两次结果会叠在同一张表里，
> 样本数翻倍、吞吐量被稀释成假数字。跑完只看 **`GET /api/org/tree`** 这一行的数据，
> **忽略 `WARMUP` 开头的那一行**。
> （GUI 打开后**确认 OrgTree Users 线程数是 30、循环数是 50**，不要改成 100/200，否则连接池被冲垮、P95 差异被排队淹没）

### 方式一：GUI（直观，适合调参 / 看实时图）

1. 启动 JMeter：`jmeter.bat`（Windows）或 `jmeter`（Mac/Linux）
2. **File → Open**，选 `docs/benchmark/org-tree-bench.jmx`
3. 双击 **OrgTree Users** 线程组，**确认 Number of Threads 是 `30`**（默认已是 30，
   不要改成 100/200；并发过大会把 HikariCP 10 连接池打满，见第四节说明）
4. **点扫帚「Clear All」清空旧报告**，再点绿色 ▶ 运行
5. 跑完后**只看 Summary / Aggregate Report 里 `GET /api/org/tree` 那一行**的
   **95% Line**（即 P95）与 **Throughput**（吞吐），`WARMUP` 行忽略不读

### 方式二：CLI（出可截图 HTML 报告，推荐提交 GitHub）

```bash
jmeter -n -t docs/benchmark/org-tree-bench.jmx \
       -l docs/benchmark/result.csv \
       -e -o docs/benchmark/report
```

- `-e -o report` 会生成带 P95 / P99 图表的可视化报告，打开 `report/index.html` 截图放 README 很加分
- 想换并发数：`jmeter -n -t ... -Jthreads=200 ...`（前提是 jmx 支持该属性；否则用 GUI 改线程数更稳）

---

## 四、为什么并发要降到 30（而不是 100/200）？

HikariCP 默认连接池只有 **10** 个。这是上一轮（100 并发）翻车的根源：

- **100 并发抢 10 个 DB 连接** → 90 个请求在连接池队列里排队 →
  响应时间被「排队等待」主导，无缓存 P95 飙到 31,740ms、有缓存也照样 31,582ms
  （你第三轮的实测）。缓存带来的「少查一次 DB」收益被排队完全淹没，P95 拉不开差距。
- **降到 30 并发（≈3 倍连接池）**：排队可控，此时
  - 无缓存：每次都查 DB（2000 行查询 ~几十~几百 ms）→ P95 较高
  - 有缓存：命中请求直接 Redis 返回（~十几 ms）→ P95 明显更低
  - 缓存的真实收益才能显现，且错误率会趋近 0%。

> 一句话：并发太高测的是「连接池排队」，不是「缓存收益」。30 是让二者都合理的甜点。

---

## 五、缓存命中率

场景 B（开缓存、已预热）跑完 JMeter 后，看 Redis 统计：

> ⚠️ `keyspace_hits/misses` 是 **Redis 自启动以来的累计值**，`flushall` 只删 key、不清计数。
> 所以**每轮压测前建议重启 Redis 服务**（Windows 关掉 redis-server 窗口再重开），让统计从 0 开始；
> 或记录压测前后的差值自己相减。否则命中率会被上一轮污染。

```bash
# Git Bash / WSL / Linux / Mac（有 grep）：
redis-cli INFO stats | grep -E "keyspace_hits|keyspace_misses"

# Windows cmd（没有 grep，改用 findstr）：
redis-cli INFO stats | findstr /C:"keyspace_hits" /C:"keyspace_misses"
```

> 提示：你本地是 **Windows cmd**（Redis 5.0.10），用上面的 `findstr` 那行即可。
> Redis 服务端 5.x 与缓存代码（Spring 客户端序列化）无关，`INFO stats` 的命中/未命中统计同样有效，无需升级。
> 最省事的办法：直接 `redis-cli INFO stats` 全量输出，自己找 `keyspace_hits` / `keyspace_misses` 两行也行。

```
# 示例输出
keyspace_hits:987
keyspace_misses:13
```

命中率 = `hits / (hits + misses)` = `987 / 1000 = 98.7%`

（命中率高低取决于你数据量和并发模式，跑出多少写多少，≥90% 即达标。）

---

## 六、EXPLAIN 索引前后对比

见同目录 `explain-demo.sql`，照着在 MySQL 里执行即可看到：

- **删索引后**：`EXPLAIN SELECT * FROM dept WHERE tenant_id = 1;` → `type = ALL`（全表扫描）
- **加回索引后**：`type = ref`，`key = idx_dept_tenant`，`rows` 大幅下降

> 注：`dept` 表目前种子数据很少，索引对延迟影响有限，这里主要是**证明你会用 EXPLAIN
> 看执行计划、懂索引选型**——面试被问「为什么加这个索引」你能当场复现。

---

## 七、把数字回填

1. 打开同目录 `RESULTS.md`，填入你实测的 P95 / 吞吐 / 命中率 / EXPLAIN 结果。
2. 打开简历（你本地那份 `优化简历_曼波.md` / 投递版），把占位数字改成实测值。
3. 把 `result.csv` / `report/` 截图 / EXPLAIN 截图放进 `docs/benchmark/`，GitHub 仓库即具备完整证据链。
