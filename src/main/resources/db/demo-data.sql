-- ============================================================
-- 演示用轻量数据（区别于 seed-data.sql 的压测大数据）
-- 目的：让前端「审批中心 / 员工管理 / 仪表盘」开箱即有内容可看。
-- 使用方式（任选）：
--   1) 本机 MySQL 客户端：mysql -u<user> -p mt_db < demo-data.sql
--   2) 客户端里 SOURCE 本文件
-- 说明：
--   * 本脚本只 INSERT，不 TRUNCATE，可重复执行（重复跑会产生重复行，
--     想重置请先手动 DELETE FROM approval_task / employee 或重跑 data.sql）。
--   * SQL 脚本不走 MyBatis-Plus 租户插件，tenant_id 必须显式写入，
--     否则运行时接口按当前租户隔离查询会查不到。
-- ============================================================
USE mt_db;

-- 演示审批单：分租户，覆盖 PENDING / APPROVED / REJECTED 三种状态
INSERT INTO approval_task(tenant_id, title, applicant, status, node_types, create_time) VALUES
  (1, '差旅报销 1200 元',       '张三', 'PENDING',   'LEADER,HR',         NOW()),
  (1, '采购测试服务器 50000 元', '李四', 'PENDING',   'LEADER,HR,FINANCE', NOW()),
  (1, '部门团建费用 3000 元',    '张三', 'APPROVED',  'LEADER,HR',         NOW()),
  (2, '客户招待费 2000 元',      '王五', 'PENDING',   'LEADER,FINANCE',    NOW()),
  (2, '办公用品采购 800 元',      '王五', 'REJECTED',  'LEADER',            NOW());

-- 再补两名员工，丰富「员工管理」演示（dept_id 引用 data.sql 中的部门）
INSERT INTO employee(tenant_id, name, dept_id, title) VALUES
  (1, '赵六', 2, '后端工程师'),
  (2, '孙七', 6, '销售主管');
