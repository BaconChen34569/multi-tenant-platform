package com.example.mt.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.mt.common.tenant.TenantContext;
import com.example.mt.entity.Employee;
import com.example.mt.mapper.EmployeeMapper;
import com.example.mt.service.EmployeeQueryService;
import com.example.mt.service.EmployeeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 员工业务层：
 *  - 读：复用 EmployeeQueryService 的 Redis 全量缓存，关键字过滤 + 分页都在内存完成，
 *    仅缓存未命中时才查一次 MySQL 全量，之后所有搜索/翻页零 MySQL 开销；
 *  - 写（增删改）：按当前租户精确失效 employees 缓存，保证缓存与库一致，
 *    且不误清其他租户（相比 allEntries=true 更契合多租户隔离语义）。
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final EmployeeQueryService employeeQueryService;
    private final StringRedisTemplate stringRedisTemplate;

    public EmployeeServiceImpl(EmployeeMapper employeeMapper,
                               EmployeeQueryService employeeQueryService,
                               StringRedisTemplate stringRedisTemplate) {
        this.employeeMapper = employeeMapper;
        this.employeeQueryService = employeeQueryService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public IPage<Employee> page(String keyword, long page, long size) {
        // 命中 Redis 缓存的全量列表（仅缓存未命中时才查一次 MySQL 全量）
        List<Employee> all = employeeQueryService.findAll();
        // 关键字过滤在内存完成，零 MySQL 开销
        List<Employee> filtered = all;
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim().toLowerCase();
            filtered = all.stream()
                    .filter(e -> e.getName() != null && e.getName().toLowerCase().contains(k))
                    .collect(Collectors.toList());
        }
        long total = filtered.size();
        int from = (int) Math.max(0, (page - 1) * size);
        int to = (int) Math.min(from + size, total);
        List<Employee> records = from >= total ? new ArrayList<>() : new ArrayList<>(filtered.subList(from, to));

        Page<Employee> p = new Page<>(page, size, total);
        p.setRecords(records);
        return p;
    }

    @Override
    public Long add(Employee emp) {
        employeeMapper.insert(emp);
        // 先落库再清缓存，避免中途异常导致缓存被清而库未写
        evictByTenant();
        return emp.getId();
    }

    @Override
    public void update(Employee emp) {
        employeeMapper.updateById(emp);
        evictByTenant();
    }

    @Override
    public void delete(Long id) {
        employeeMapper.deleteById(id);
        evictByTenant();
    }

    /**
     * 按租户精确失效：仅删除当前租户在 employees 缓存下的 key
     * （pattern = "employees::&lt;tenant&gt;"），不影响其他租户缓存。
     * 此处用 keys 精确匹配即可满足演示数据量；生产大 key 量场景可改为
     * SCAN 游标，避免 KEYS 命令在海量 key 时阻塞 Redis。
     */
    private void evictByTenant() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            return;
        }
        String pattern = "employees::" + tenantId;
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
