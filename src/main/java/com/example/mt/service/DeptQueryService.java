package com.example.mt.service;

import com.example.mt.common.tenant.TenantContext;
import com.example.mt.entity.Dept;
import com.example.mt.mapper.DeptMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 部门只读查询的 Redis 缓存层。
 *
 * 缓存「当前租户的全量部门列表」（不带关键字）。关键字过滤与分页都在调用方
 * DeptServiceImpl 基于这份缓存结果做内存处理，避免每次搜索都打 MySQL 的 LIKE 查询，
 * 仅缓存未命中时才查一次 MySQL 全量。
 */
@Component
public class DeptQueryService {

    private final DeptMapper deptMapper;

    public DeptQueryService(DeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @Cacheable(cacheNames = "orgTree", key = "T(com.example.mt.common.tenant.TenantContext).get()")
    public List<Dept> findAll() {
        // tenant_id 条件由 MyBatis-Plus 租户插件自动注入，业务代码零侵入
        return deptMapper.selectList(null);
    }
}
