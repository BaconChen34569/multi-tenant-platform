-- ============================================================
-- 多租户协同办公平台 · 数据库一键初始化脚本
-- 合并自 schema.sql（建库建表 + 复合索引）+ data.sql（租户/用户/部门/员工种子）
--        + demo-data.sql（演示用审批单与补充员工）
-- 只需执行本文件一次，即可得到完整可运行的数据库环境。
-- 用法：
--   mysql -u<user> -p<password> < src/main/resources/db/init.sql
-- 说明：
--   * 本脚本会 CREATE DATABASE IF NOT EXISTS mt_db，已存在则复用；
--   * 重复执行会在 demo-data 段产生重复审批单（data 段因显式 id 会主键冲突），
--     如需重置请先 DROP DATABASE mt_db 或手动清理相关表。
--   * 压测大数据不在此文件内，见同目录 seed-data.sql（会清空并灌入近万条数据）。
-- ============================================================


-- ------------------------------------------------------------
-- 1) 建库建表 + 复合索引（原 schema.sql）
-- ------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS mt_db DEFAULT CHARACTER SET utf8mb4;
USE mt_db;

CREATE TABLE sys_tenant (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_name VARCHAR(64),
    code        VARCHAR(32)
);

CREATE TABLE sys_user (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(64),
    password  VARCHAR(64),
    tenant_id BIGINT,
    role      VARCHAR(32)
);

CREATE TABLE dept (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '隔离列',
    name      VARCHAR(64),
    parent_id BIGINT
);

CREATE TABLE employee (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL COMMENT '隔离列',
    name      VARCHAR(64),
    dept_id   BIGINT,
    title     VARCHAR(64)
);

CREATE TABLE approval_task (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id  BIGINT NOT NULL COMMENT '隔离列',
    title      VARCHAR(128),
    applicant  VARCHAR(64),
    amount     DECIMAL(12,2) DEFAULT 0 COMMENT '申请金额',
    status     VARCHAR(32),
    node_types VARCHAR(128),
    execution_log TEXT COMMENT '提交时责任链执行的节点级轨迹',
    create_time DATETIME
);

-- 复合索引：租户维度查询加速（对应简历“通过 Explain 重建复合索引”）
CREATE INDEX idx_dept_tenant        ON dept(tenant_id);
CREATE INDEX idx_employee_tenant    ON employee(tenant_id);
CREATE INDEX idx_approval_tenant    ON approval_task(tenant_id, status);


-- ------------------------------------------------------------
-- 2) 种子数据：租户 / 用户 / 部门 / 员工（原 data.sql）
-- ------------------------------------------------------------
USE mt_db;

-- 两个租户
INSERT INTO sys_tenant(id, tenant_name, code) VALUES
    (1, '租户A-科技公司', 'TENANT_A'),
    (2, '租户B-贸易公司', 'TENANT_B');

-- 用户（演示登录用）
INSERT INTO sys_user(id, username, password, tenant_id, role) VALUES
    (1, 'alice', '123', 1, 'ADMIN'),
    (2, 'bob',   '123', 2, 'ADMIN');

-- 租户A 部门树
INSERT INTO dept(id, tenant_id, name, parent_id) VALUES
    (1, 1, '总部',   0),
    (2, 1, '研发部', 1),
    (3, 1, '产品部', 1),
    (4, 1, '研发一部', 2);

-- 租户B 部门树（id 与 A 不同，用来验证“零串扰”）
INSERT INTO dept(id, tenant_id, name, parent_id) VALUES
    (5, 2, '分公司', 0),
    (6, 2, '销售部', 5);

-- 员工
INSERT INTO employee(id, tenant_id, name, dept_id, title) VALUES
    (1, 1, '张三', 2, '工程师'),
    (2, 1, '李四', 3, '产品经理'),
    (3, 2, '王五', 6, '销售');


-- ------------------------------------------------------------
-- 3) 演示数据：审批单 + 补充员工（原 demo-data.sql）
--    仅 INSERT，不 TRUNCATE；dept_id 引用第 2 段中的部门 id。
-- ------------------------------------------------------------
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

-- 再补两名员工，丰富「员工管理」演示（dept_id 引用第 2 段的部门）
INSERT INTO employee(tenant_id, name, dept_id, title) VALUES
  (1, '赵六', 2, '后端工程师'),
  (2, '孙七', 6, '销售主管');
