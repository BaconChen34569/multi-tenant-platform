# 压测结果（真实回填 · 2026-07-23 第六轮）

> 本文件由本地**真实跑出后**填写，对应简历里的缓存优化指标。
> 压测脚本：`docs/benchmark/org-tree-bench.jmx`（含 Warmup 预热组 + 正式压测组，serialize_threadgroups=true）。

## 1. 环境

| 项 | 值 |
|---|---|
| CPU | _（你的本地 CPU，可补）_ |
| 内存 | _（如 16G，可补）_ |
| MySQL | 8.x |
| Redis | 5.0.10（本机） |
| 压测并发 | 30 线程 |
| 循环 / 总请求 | 50 循环 / 场景 1500 请求（Warmup 另 450 请求不计入正式指标） |
| 数据量 | `seed-data.sql` 灌入 2000 部门 + 6000 员工 |

## 2. 缓存前后对比（JMeter 跑 org-tree-bench.jmx，30 线程 × 50 循环）

| 场景 | P50 | P95 | P99 | 平均 | 吞吐 (req/s) |
|---|---|---|---|---|---|
| 无缓存（SPRING_CACHE_TYPE=none） | 468 ms | 524 ms | 590 ms | 452 ms | 63.3 |
| 有缓存（Redis） | 6 ms | 8 ms | 10 ms | 5 ms | 1237.6 |

> 结论一句话：缓存使 P95 由 **524ms 降至 8ms**（↓98.5%，约 65 倍），平均响应 452ms → 5ms，吞吐量由 63 QPS 提升至 1237 QPS（↑19.5 倍）。

## 3. 缓存命中率（场景 B 跑完后 `redis-cli INFO stats`）

```
keyspace_hits:   1927
keyspace_misses:   23
```

命中率 = hits / (hits + misses) = 1927 / 1950 = **98.8%**

> 说明：总请求 = Warmup 450 + 正式 1500 = 1950；misses 23 来自首次冷加载与少量并发竞争，符合预热后表现。

## 4. EXPLAIN 索引对比（dept 表）

| 状态 | type | key | rows |
|---|---|---|------|
| 删索引后 | ALL | NULL | 2800 |
| 加回 idx_dept_tenant | ref | idx_dept_tenant | 2000 |

> 注：小数据量下 rows 差异不大，重点是证明会用 EXPLAIN 看执行计划、懂索引选型；面试被问「为什么加这个索引」可当场复现。

## 5. 证据附件（放进本目录，提交到 GitHub）

- [x] `org-tree-bench.jmx`（压测脚本，含 Warmup + 正式两组）
- [x] JMeter Summary / Aggregate 报告截图（场景 A、场景 B 各一张，见 `docs/results/`）
- [x] Redis `INFO stats` 命中率截图（见 `docs/results/Report-with_SPRING_CACHE/`）
- [ ] EXPLAIN 前后截图（可补）
