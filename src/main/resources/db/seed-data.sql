-- ============================================================
-- 压测专用：中等数据量灌入脚本（方案 A'）
-- 目的：让部门树查询（SELECT * FROM dept WHERE tenant_id=?）
--       在「无缓存」时确实变慢（几十~几百 ms），但又不至于把
--       HikariCP 默认 10 连接池打满到离谱地步，
--       从而让 Redis 缓存的真实收益（P95 差距）能够显现。
-- 直接在本机 MySQL 客户端执行：mysql -u<user> -p mt_db < seed-data.sql
-- 或客户端里 SOURCE 本文件。
-- 说明：本脚本先 TRUNCATE 清掉旧数据（含 data.sql 的 10 条基础部门，
--       压测不需要它们；重跑可覆盖上一次的大数据，保持纯净），
--       再灌入新的中等数据量。前端演示（租户隔离）仍可用，数据更多更真实。
-- ============================================================
USE mt_db;
-- 递归 CTE 生成序列需要更高上限
SET SESSION cte_max_recursion_depth = 100000;
-- 清旧数据（先 employee 再 dept，避免外键约束报错）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE employee;
TRUNCATE TABLE dept;
SET FOREIGN_KEY_CHECKS = 1;

-- 1) 租户1：2000 个部门，树形结构
--    根节点 10 个，其余约每 20 个挂一层，形成 3~4 层组织树
INSERT INTO dept (id, tenant_id, name, parent_id)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 2000
)
SELECT
  100000 + n                              AS id,
  1                                       AS tenant_id,
  CONCAT('部门-', 100000 + n)             AS name,
  CASE WHEN n <= 10 THEN 0
       ELSE 100000 + FLOOR((n - 11) / 20) + 1
  END                                     AS parent_id
FROM seq;

-- 2) 租户2：800 个部门（验证中等数据量下「零串扰」）
INSERT INTO dept (id, tenant_id, name, parent_id)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 800
)
SELECT
  200000 + n                              AS id,
  2                                       AS tenant_id,
  CONCAT('部门-', 200000 + n)             AS name,
  CASE WHEN n <= 5 THEN 0
       ELSE 200000 + FLOOR((n - 6) / 20) + 1
  END                                     AS parent_id
FROM seq;

-- 3) 租户1：6000 名员工，随机挂到部门树上
INSERT INTO employee (tenant_id, name, dept_id, title)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 6000
)
SELECT
  1                                       AS tenant_id,
  CONCAT('员工-', n)                      AS name,
  100000 + (FLOOR(RAND() * 2000) + 1)  AS dept_id,
  '工程师'                                 AS title
FROM seq;

-- 4) 租户2：1500 名员工
INSERT INTO employee (tenant_id, name, dept_id, title)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 1500
)
SELECT
  2                                       AS tenant_id,
  CONCAT('员工-', n)                      AS name,
  200000 + (FLOOR(RAND() * 800) + 1)   AS dept_id,
  '销售'                                   AS title
FROM seq;

-- 5) 为 employee.dept_id 建索引，支撑上面的分组 JOIN
--    （演示一次索引优化；已存在则跳过）
DROP PROCEDURE IF EXISTS mt_add_idx;
DELIMITER $$
CREATE PROCEDURE mt_add_idx()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = 'mt_db'
      AND table_name  = 'employee'
      AND index_name  = 'idx_employee_dept'
  ) THEN
    ALTER TABLE employee ADD INDEX idx_employee_dept (dept_id);
  END IF;
END$$
DELIMITER ;
CALL mt_add_idx();
DROP PROCEDURE mt_add_idx;
