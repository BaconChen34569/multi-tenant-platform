package com.example.mt.common.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.List;

/**
 * 多租户 SQL 改写处理器：交给 MyBatis-Plus 的 TenantLineInnerInterceptor 调用。
 * 它会在每条 SQL 的 WHERE 后自动拼接 `tenant_id = ?`，参数取自 TenantContext。
 * 这正是简历里“基于 MyBatis 拦截器实现 SQL 自动改写、业务层零侵入”的真实实现。
 */
public class TenantHandler implements TenantLineHandler {

    private static final String TENANT_COLUMN = "tenant_id";
    private static final List<String> IGNORE_TABLES = List.of("sys_tenant", "sys_user");

    @Override
    public Expression getTenantId() {
        Long id = TenantContext.get();
        if (id == null) {
            // 未带租户时默认 0，避免空指针；生产应拒绝无租户请求
            return new LongValue(0L);
        }
        return new LongValue(id);
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 租户表、用户表本身不做隔离
        return IGNORE_TABLES.contains(tableName);
    }
}
