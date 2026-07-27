package com.example.mt.service.impl;

import com.example.mt.common.tenant.TenantContext;
import com.example.mt.entity.Dept;
import com.example.mt.entity.DeptTreeNode;
import com.example.mt.mapper.DeptMapper;
import com.example.mt.service.DeptQueryService;
import com.example.mt.service.DeptService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl implements DeptService {

    private final DeptMapper deptMapper;
    private final DeptQueryService deptQueryService;

    public DeptServiceImpl(DeptMapper deptMapper, DeptQueryService deptQueryService) {
        this.deptMapper = deptMapper;
        this.deptQueryService = deptQueryService;
    }

    /**
     * 组织架构列表：直接复用 DeptQueryService 的 Redis 全量缓存（cacheName=orgTree）。
     */
    @Override
    public List<Dept> tree() {
        return deptQueryService.findAll();
    }

    /**
     * 带统计的部门树：递归 CTE + 员工分组，属于「重查询」。
     * 用独立缓存命名空间 orgTreeStats，按租户隔离。
     */
    @Override
    @Cacheable(cacheNames = "orgTreeStats", key = "T(com.example.mt.common.tenant.TenantContext).get()")
    public List<DeptTreeNode> treeWithStats() {
        Long tenantId = TenantContext.get();
        return deptMapper.selectTreeWithStats(tenantId);
    }

    /**
     * 部门分页：复用 orgTree 缓存的全量列表，在内存中做关键字过滤 + 切片分页，
     * 仅缓存未命中时才查一次 MySQL 全量，之后搜索/翻页零 MySQL 开销。
     */
    @Override
    public IPage<Dept> page(long page, long size, String keyword) {
        List<Dept> all = deptQueryService.findAll();
        List<Dept> filtered = all;
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim().toLowerCase();
            filtered = all.stream()
                    .filter(d -> d.getName() != null && d.getName().toLowerCase().contains(k))
                    .collect(Collectors.toList());
        }
        long total = filtered.size();
        int from = (int) Math.max(0, (page - 1) * size);
        int to = (int) Math.min(from + size, total);
        List<Dept> records = from >= total ? new ArrayList<>() : new ArrayList<>(filtered.subList(from, to));

        Page<Dept> p = new Page<>(page, size, total);
        p.setRecords(records);
        return p;
    }
}
