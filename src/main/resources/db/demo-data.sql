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

-- 演示审批单：分租户，覆盖 PENDING / APPROVED / REJECTED 三种状态。
-- amount 与 execution_log 一并写入，使历史页开箱即可展示金额与责任链轨迹，
-- 其内容与运行时责任链 + /decide 产出保持一致（财务阈值 5000：>5000 走审批、否则免审）。
INSERT INTO approval_task(tenant_id, title, applicant, amount, status, node_types, execution_log, create_time) VALUES
  (1, '差旅报销 1200 元',       '张三', 1200.00,  'PENDING',   'LEADER,HR',
      '主管环节已处理; 人事环节已处理',                                                          NOW()),
  (1, '采购测试服务器 50000 元', '李四', 50000.00, 'PENDING',   'LEADER,HR,FINANCE',
      '主管环节已处理; 人事环节已处理; 财务环节已处理(金额 50000.0)',                              NOW()),
  (1, '部门团建费用 3000 元',    '张三', 3000.00,  'APPROVED',  'LEADER,HR',
      '主管环节已处理; 人事环节已处理 → 最终审批：已通过',                                        NOW()),
  (2, '客户招待费 2000 元',      '王五', 2000.00,  'PENDING',   'LEADER,FINANCE',
      '主管环节已处理; 财务免审(金额 2000.0)',                                                    NOW()),
  (2, '办公用品采购 800 元',      '王五', 800.00,   'REJECTED',  'LEADER',
      '主管环节已处理 → 最终审批：已驳回',                                                        NOW());

-- 再补两名员工，丰富「员工管理」演示（dept_id 引用 data.sql 中的部门）
INSERT INTO employee(tenant_id, name, dept_id, title) VALUES
  (1, '赵六', 2, '后端工程师'),
  (2, '孙七', 6, '销售主管');
