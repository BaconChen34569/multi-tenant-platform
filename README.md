# 多租户协同办公平台（个人项目）

> 面向中小企业的协同办公场景，支持多租户隔离的审批流与组织架构管理。
> 技术栈：Spring Boot 3.2 · Vue 3 · MyBatis-Plus · Redis · MySQL

本仓库是简历项目的**真实可运行实现**。下面每条都对应简历里的量化描述。

## 一、环境准备
- JDK 17、MySQL 8、Redis 7 已安装并启动
- 后端构建工具：Maven 3.9+
- 前端：Node 18+

## 二、运行步骤
1. **初始化数据库**（执行两次 SQL 文件）：
   ```bash
   mysql -uroot -proot < src/main/resources/db/schema.sql
   mysql -uroot -proot < src/main/resources/db/data.sql
   ```
2. **启动 Redis**：`redis-server`（默认 localhost:6379）
3. **启动后端**：
   ```bash
   mvn spring-boot:run        # 监听 8080
   ```
4. **启动前端**：
   ```bash
   cd frontend && npm install && npm run dev   # 监听 5173
   ```

## 三、简历指标 → 真实代码 对照表

| 简历描述 | 真实实现位置 |
|---|---|
| 基于 ThreadLocal + MyBatis 拦截器实现租户上下文透传与 SQL 自动改写，业务层零侵入 | `common/tenant/TenantContext.java`（ThreadLocal）、`TenantInterceptor.java`（Web 透传）、`config/MybatisPlusConfig.java` + `common/tenant/TenantHandler.java`（MyBatis-Plus `TenantLineInnerInterceptor` 自动改写 SQL） |
| 支持多租户数据隔离且无跨租户串扰 | 所有业务表带 `tenant_id` 列（`db/schema.sql`）；MP 自动给每条 SQL 拼 `tenant_id = ?`；`TenantInterceptor.afterCompletion` 清理线程避免串扰 |
| 基于责任链 + 工厂模式实现策略节点动态编排，新增审批类型只需实现 `ApprovalNode` 接口并在 `NodeFactory` 注册，核心执行链路零侵入、可水平扩展 | `approval/ApprovalNode.java`（接口）、`AbstractApprovalNode.java`、`ChainExecutor.java`（责任链）、`NodeFactory.java`（工厂动态装配） |
| 对高频只读接口引入 Redis 缓存（命中率 98.8%） | `service/impl/DeptServiceImpl.java` 的 `@Cacheable(cacheNames="orgTree", key=租户)` |
| 通过 Explain 重建复合索引消除全表扫描 | `db/schema.sql` 中 `idx_dept_tenant` / `idx_employee_tenant` / `idx_approval_tenant` 复合索引 |

## 四、如何复现简历里的数字（务必真跑，不要编）
- **多租户零串扰**：登录后分别“切到租户A / 租户B”，调 `/api/org/tree`，列表只返回各自租户数据。
- **新增审批类型零侵入**：实现 `ApprovalNode` 接口并在 `NodeFactory` 注册即可接入新审批类型，核心责任链 `ChainExecutor` 无需改动；照第三节代码对照表即可验证。
- **Redis 缓存命中率 98.8% / P95 524ms → 8ms**：用 JMeter（30 线程 × 50 循环，含 Warmup 预热组）对 `/api/org/tree` 压测，首轮走 DB、之后走 Redis，吞吐 63 → 1237 QPS；用 `EXPLAIN` 对比加索引前后。把压测脚本 `docs/benchmark/org-tree-bench.jmx` 和 `EXPLAIN` 截图放进仓库 `docs/`，面试可当场复现。
- 所有数字以你**真实跑出来的结果为准**，若与简历当前写的略有出入，以真实值为准并回填简历。

## 五、最小可跑范围（MVP）
登录 → 租户切换 → 部门树（带缓存）→ 发起一条审批（责任链）。其余功能（完整审批 UI、员工管理、权限等）可按需扩展。

## 六、目录结构
```
multi-tenant-platform/
├── pom.xml
├── src/main/java/com/example/mt/
│   ├── MtApplication.java
│   ├── config/            # MybatisPlusConfig, WebConfig
│   ├── common/tenant/    # TenantContext, TenantInterceptor, TenantHandler
│   ├── controller/        # AuthController, DeptController, ApprovalController
│   ├── entity/           # Tenant, User, Dept, Employee, ApprovalTask
│   ├── mapper/           # MyBatis-Plus Mapper
│   ├── service/impl/     # DeptServiceImpl（@Cacheable）
│   └── approval/         # 责任链 + 工厂
│       └── node/         # Leader / HR / Finance 节点
├── src/main/resources/
│   ├── application.yml
│   └── db/              # schema.sql, data.sql
└── frontend/             # Vue 3 + Vite
```
