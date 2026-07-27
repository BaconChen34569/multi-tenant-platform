# 多租户协同办公平台

> 面向中小企业的协同办公场景，支持多租户数据隔离的审批流、组织架构与员工管理。
> 技术栈：Spring Boot 3.2.5 · Vue 3 + Vite（零 UI 库，自建中台设计系统）· MyBatis-Plus 3.5.7 · Redis · MySQL

一个端到端可运行的个人项目，重点展示**多租户隔离、责任链审批编排、Redis 缓存策略与查询性能优化**四项工程能力。

---

## 一、核心功能

| 模块 | 能力 |
|---|---|
| 多租户隔离 | ThreadLocal + MyBatis-Plus 租户拦截器，自动给每条 SQL 注入 `tenant_id`；前端经 `X-Tenant-Id` 请求头透传，业务代码零侵入 |
| 数据概览 | 当前租户的部门数 / 员工数 / 待审数 / 已批数统计 |
| 组织部门 | 部门树、带统计的重查询接口（压测用）、分页列表（支持部门名搜索），全量缓存 + 内存过滤 |
| 员工管理 | 分页列表 + 姓名搜索 + 增删改（CRUD），全量缓存 + 内存过滤，写操作按租户精确失效 |
| 审批中心 | 责任链 + 工厂模式；提交即跑链并落库轨迹、人工签批写回同一根轨迹、待办与历史查询 |

---

## 二、架构设计亮点

**1. 多租户隔离（零业务侵入）**
- `TenantContext`（ThreadLocal）保存当前租户，`TenantInterceptor` 从请求头 `X-Tenant-Id` 透传；
- `TenantLineInnerInterceptor` 在 SQL 执行前自动改写，为每条查询拼上 `tenant_id = ?`，`afterCompletion` 清理线程避免串扰；
- 切换租户时列表仅返回该租户数据，可直观验证隔离正确性。

**2. 审批责任链 + 工厂**
- `ApprovalNode` 定义节点契约，`NodeFactory` 按配置动态装配节点成链，`ChainExecutor` 顺序执行；
- 新增审批类型只需实现 `ApprovalNode` 并在工厂注册，核心链路无需改动；
- 提交时即执行责任链，节点级处理结果写入 `execution_log`；人工签批时把「最终审批」结论追加进同一条轨迹，避免「链显示通过、状态却驳回」的矛盾。

**3. Redis 缓存策略（绕开 MySQL）**
- 部门 / 员工查询各自建立缓存层（`orgTree` / `employees`），缓存键绑定租户，天然隔离；
- 缓存「当前租户的全量列表」，关键字过滤与分页均在应用内存完成 —— 同一租户下任意搜索、翻页都命中 Redis，**不重复查库**；
- 员工写操作按 `employees::<租户>` 前缀**精确失效**当前租户缓存（用 `StringRedisTemplate` 而非 `allEntries`），不影响其他租户。

**4. 索引优化**
- `approval_task`、`dept`、`employee` 均建 `tenant_id` 复合索引，消除租户维度查询的全表扫描。

---

## 三、本地运行

前置：JDK 17、MySQL 8、Redis 7、Maven 3.9+、Node 18+。

```bash
# 1. 初始化数据库（建表 + 复合索引 + 种子数据 + 演示数据，一键完成）
mysql -u<user> -p<password> < src/main/resources/db/init.sql

# 2. 启动 Redis（需确保可达）
redis-server

# 3. 启动后端（默认 8080）
mvn spring-boot:run

# 4. 启动前端（默认 5173）
cd frontend && npm install && npm run dev
```

- 访问 `http://localhost:5173`，使用 `data.sql` 中预置的演示账号登录即可体验。
- 敏感配置（数据库 / Redis 连接信息）通过环境变量注入，仓库提供 `application-example.yml` 作为配置模板，**不提交任何明文凭据**。

### SQL 文件说明

`src/main/resources/db/` 下的一组 SQL 文件按"职责拆分、可独立执行"，其中 `init.sql` 是它们的合并产物，便于一键初始化：

| 文件 | 作用 | 是否并入 init.sql |
|---|---|---|
| `init.sql` | **一键初始化入口**：依次整合下方三个脚本（建表 + 复合索引 + 种子数据 + 演示数据），执行一次即得完整可运行环境 | —（自身为合并结果） |
| `schema.sql` | 建库 `mt_db` 与五张业务表（`tenant` / `user` / `dept` / `employee` / `approval_task`）的 DDL，含 `tenant_id` 复合索引 | ✅ 第 1 段 |
| `data.sql` | 种子数据：两个租户、预置演示账号、A/B 租户部门树与基础员工，是平台运行的最小数据集 | ✅ 第 2 段 |
| `demo-data.sql` | 演示数据：5 条覆盖 PENDING / APPROVED / REJECTED 的审批单 + 2 名补充员工，用于直观呈现审批中心与仪表盘 | ✅ 第 3 段 |
| `seed-data.sql` | **压测专用**：先 `TRUNCATE` 部门 / 员工表再灌入近万条数据，配合 JMeter 复现「缓存命中 98.8%、P95 524ms→8ms」的性能收益 | ❌ 排除（会清空演示数据，破坏开箱体验） |

> 仅想建表不导数据时，可单独执行 `schema.sql`；需要复现缓存收益时，再单独执行 `seed-data.sql` 并运行压测脚本。

---

## 四、能力点 → 代码实现对照

| 能力描述 | 真实实现位置 |
|---|---|
| ThreadLocal + 拦截器实现租户上下文透传与 SQL 自动改写，业务零侵入 | `common/tenant/TenantContext.java`、`TenantInterceptor.java`、`config/MybatisPlusConfig.java` + `common/tenant/TenantHandler.java` |
| 多租户隔离且无跨租户串扰 | 业务表均带 `tenant_id`；MP 自动拼条件；`TenantInterceptor.afterCompletion` 清理线程 |
| 责任链 + 工厂动态编排，新增类型零侵入 | `approval/ApprovalNode.java`、`ChainExecutor.java`、`NodeFactory.java`、`approval/node/`（Leader / HR / Finance） |
| 提交即跑链、节点级轨迹落库、历史可回溯 | `ApprovalController.submit()` 写 `execution_log`；`decide()` 追加「最终审批」结论 |
| 高频只读接口引入 Redis 缓存 | `DeptQueryService.findAll()` / `EmployeeQueryService.findAll()` 的 `@Cacheable`（键绑定租户） |
| 缓存 + 应用层处理绕开 MySQL，翻页零查库 | `DeptServiceImpl.page()` / `EmployeeServiceImpl.page()`：全量缓存 → 内存过滤 + 切片分页 |
| 按租户粒度精确失效缓存 | `EmployeeServiceImpl.evictByTenant()` 用 `StringRedisTemplate` 按前缀精确删除 |
| 复合索引消除全表扫描 | `db/schema.sql` 的 `idx_dept_tenant` / `idx_employee_tenant` / `idx_approval_tenant` |
| 前端对标大厂中台、零依赖 | `styles/index.css`（CSS 变量设计系统）、`components/IconSvg.vue`（线性 SVG 图标）、`App.vue`（侧边栏 + 顶栏 + 租户切换） |

---

## 五、设计取舍

- 缓存失效采用 `KEYS` 前缀扫描，演示数据量下无压力；若 key 规模增大，可平滑替换为 `SCAN` 游标避免阻塞 Redis。
- 预置演示数据（`demo-data.sql` / `init.sql` 第 3 段）已一并写入 `amount` 与 `execution_log`，历史页开箱即展示金额与责任链轨迹；其内容与运行时责任链 + `/decide` 产出保持一致（财务阈值 5000：金额超阈值走审批、否则免审）。
- 登录为演示态，未接入 JWT / 权限矩阵；本项目重点展示多租户隔离机制而非鉴权体系。
- 定位为单机演示项目，重点在隔离机制、缓存策略与架构清晰度的呈现。

---

## 六、目录结构

```
multi-tenant-platform/
├── pom.xml
├── src/main/java/com/example/mt/
│   ├── common/tenant/      # TenantContext, TenantInterceptor, TenantHandler
│   ├── config/             # MybatisPlusConfig, RedisConfig, WebConfig
│   ├── controller/         # Auth, Dept, Employee, Approval, Dashboard
│   ├── entity/             # Tenant, User, Dept, Employee, ApprovalTask, DeptTreeNode
│   ├── mapper/             # MyBatis-Plus Mapper
│   ├── service/            # Dept/Employee 接口 + *QueryService 缓存层
│   │   └── impl/           # DeptServiceImpl, EmployeeServiceImpl
│   └── approval/           # 责任链 + 工厂
│       └── node/           # Leader / HR / Finance 节点
├── src/main/resources/
│   ├── application.yml           # 环境变量占位符（脱敏）
│   ├── application-example.yml   # 配置模板
│   └── db/                       # init.sql（建表+种子+演示数据，一键初始化）/ seed-data.sql（压测专用）
└── frontend/                     # Vue 3 + Vite，零 UI 库中台设计系统
    ├── api.js                    # 自动带 X-Tenant-Id 请求头
    ├── App.vue                   # 侧边栏 + 顶栏 + 租户切换
    ├── components/IconSvg.vue    # 线性 SVG 图标
    ├── styles/index.css          # CSS 变量设计系统
    ├── router.js                 # 4 个业务路由 + /login
    └── views/                    # Dashboard / OrgTree / Employee / Approval / Login
```
