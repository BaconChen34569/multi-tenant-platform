package com.example.mt.service;

import com.example.mt.entity.Dept;
import com.example.mt.entity.DeptTreeNode;

import java.util.List;

public interface DeptService {
    /** 返回当前租户的部门列表（租户条件由 MyBatis-Plus 自动注入） */
    List<Dept> tree();

    /**
     * 返回当前租户带统计信息的部门树（递归 CTE + 员工分组）。
     * 查询较重，由调用方（Controller）决定是否命中缓存。
     */
    List<DeptTreeNode> treeWithStats();
}
