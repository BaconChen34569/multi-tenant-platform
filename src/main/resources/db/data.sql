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
