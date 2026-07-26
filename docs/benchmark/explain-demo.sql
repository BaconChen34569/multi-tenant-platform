-- ============================================================
-- 索引优化 EXPLAIN 演示：删索引(优化前) → 加回索引(优化后)
-- 在 MySQL 中按顺序执行，对比两次 EXPLAIN 的 type / key / rows
-- ============================================================

USE mt_db;

-- ---------- 1) 优化前：删除索引，全表扫描 ----------
ALTER TABLE dept DROP INDEX idx_dept_tenant;

EXPLAIN SELECT * FROM dept WHERE tenant_id = 1;
-- 预期：type = ALL（全表扫描），key = NULL，rows = 全表行数
-- 说明：没有索引时，MySQL 必须逐行扫描整张表来过滤 tenant_id


-- ---------- 2) 优化后：加回复合/单列索引 ----------
ALTER TABLE dept ADD INDEX idx_dept_tenant (tenant_id);

EXPLAIN SELECT * FROM dept WHERE tenant_id = 1;
-- 预期：type = ref（索引查找），key = idx_dept_tenant，rows = 命中的少量行
-- 说明：走索引后只需定位 tenant_id 对应的行，扫描行数大幅下降


-- ---------- 3) 进阶：复合索引对「租户 + 状态」联合查询的加速 ----------
-- approval_task 上已有 (tenant_id, status) 复合索引，验证联合过滤效果
EXPLAIN SELECT * FROM approval_task WHERE tenant_id = 1 AND status = 'PENDING';
-- 预期：type = ref，key = idx_approval_tenant，rows = 少量
-- 说明：最左前缀原则生效，tenant_id + status 两个过滤条件都用上了索引
