package com.example.mt.service.impl;

import com.example.mt.common.tenant.TenantContext;
import com.example.mt.entity.Dept;
import com.example.mt.entity.DeptTreeNode;
import com.example.mt.mapper.DeptMapper;
import com.example.mt.service.DeptService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;

    public DeptServiceImpl(DeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    /**
     * 组织架构树：高频只读接口用 Redis 缓存。
     * key 绑定当前租户，保证不同租户缓存隔离。
     */
    @Override
    @Cacheable(cacheNames = "orgTree", key = "T(com.example.mt.common.tenant.TenantContext).get()")
    public List<Dept> tree() {
        // 注意：tenant_id 条件由 MyBatis-Plus 自动注入，业务代码零侵入
        return deptMapper.selectList(null);
    }

    /**
     * 带统计的部门树：递归 CTE + 员工分组，属于「重查询」。
     * 用独立缓存命名空间 orgTreeStats，同样按租户隔离。
     * 无缓存（SPRING_CACHE_TYPE=none）时每次都重算，P95 明显偏高；
     * 有缓存时首次构建后命中 Redis，P95 骤降、命中率接近 100%。
     */
    @Override
    @Cacheable(cacheNames = "orgTreeStats", key = "T(com.example.mt.common.tenant.TenantContext).get()")
    public List<DeptTreeNode> treeWithStats() {
        Long tenantId = TenantContext.get();
        return deptMapper.selectTreeWithStats(tenantId);
    }
}
