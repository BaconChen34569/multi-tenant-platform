-- 多租户协同办公平台 建库建表
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
    status     VARCHAR(32),
    node_types VARCHAR(128),
    create_time DATETIME
);

-- 复合索引：租户维度查询加速（对应简历“通过 Explain 重建复合索引”）
CREATE INDEX idx_dept_tenant        ON dept(tenant_id);
CREATE INDEX idx_employee_tenant    ON employee(tenant_id);
CREATE INDEX idx_approval_tenant    ON approval_task(tenant_id, status);
