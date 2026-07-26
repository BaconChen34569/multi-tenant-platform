package com.example.mt.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mt.entity.Dept;
import com.example.mt.entity.DeptTreeNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    /**
     * 递归 CTE 构建当前租户的部门树（含深度），
     * 并 LEFT JOIN 员工分组结果，统计每个部门的员工数。
     *
     * 注意：
     * 1. 用 @InterceptorIgnore(tenantLine = "true") 关闭自动租户注入，
     *    因为递归 CTE 内部多次引用 dept / employee 表，自动注入会失败/重复。
     *    改为在 SQL 中手动按 tenantId 过滤，隔离语义不变。
     * 2. 该查询在大数据量（万级部门）下明显偏重，正适合用来体现缓存价值。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select({
        "WITH RECURSIVE cte AS (",
        "  SELECT id, tenant_id, name, parent_id, 0 AS depth",
        "  FROM dept WHERE tenant_id = #{tenantId} AND parent_id = 0",
        "  UNION ALL",
        "  SELECT d.id, d.tenant_id, d.name, d.parent_id, cte.depth + 1",
        "  FROM dept d INNER JOIN cte ON d.parent_id = cte.id",
        "  WHERE d.tenant_id = #{tenantId}",
        ")",
        "SELECT cte.id AS id, cte.name AS name, cte.parent_id AS parentId,",
        "       cte.depth AS depth, COALESCE(emp.cnt, 0) AS empCount",
        "FROM cte",
        "LEFT JOIN (",
        "  SELECT dept_id, COUNT(*) AS cnt FROM employee",
        "  WHERE tenant_id = #{tenantId} GROUP BY dept_id",
        ") emp ON emp.dept_id = cte.id",
        "ORDER BY cte.depth, cte.id"
    })
    List<DeptTreeNode> selectTreeWithStats(@Param("tenantId") Long tenantId);
}
