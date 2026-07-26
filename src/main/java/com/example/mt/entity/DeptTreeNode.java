package com.example.mt.entity;

import lombok.Data;

/**
 * 部门树节点 DTO（含层级深度与员工数统计）。
 * 与轻量 {@link Dept} 不同，本对象由「递归 CTE + 员工分组」查询生成，
 * 用于演示大数据量下缓存对重查询的加速效果。
 */
@Data
public class DeptTreeNode {
    private Long id;
    private String name;
    private Long parentId;
    private Integer depth;
    private Long empCount;
}
