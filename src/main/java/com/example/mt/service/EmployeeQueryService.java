package com.example.mt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mt.common.tenant.TenantContext;
import com.example.mt.entity.Employee;
import com.example.mt.mapper.EmployeeMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 员工查询（只读）的 Redis 缓存层。
 *
 * 缓存「当前租户的全量员工列表」（不带关键字）。关键字过滤与分页都在调用方
 * EmployeeServiceImpl 基于这份缓存结果做内存处理——这样同一个租户只需查一次库，
 * 所有搜索/翻页都命中 Redis + 内存，零 MySQL 开销（对应简历“缓存命中 98.8%”）。
 *
 * cache key 绑定当前租户，保证多租户间缓存隔离。
 */
@Component
public class EmployeeQueryService {

    private final EmployeeMapper employeeMapper;

    public EmployeeQueryService(EmployeeMapper employeeMapper) {
        this.employeeMapper = employeeMapper;
    }

    @Cacheable(cacheNames = "employees", key = "T(com.example.mt.common.tenant.TenantContext).get()")
    public List<Employee> findAll() {
        // tenant_id 条件由 MyBatis-Plus 租户插件自动注入，业务代码零侵入
        LambdaQueryWrapper<Employee> w = new LambdaQueryWrapper<>();
        w.orderByDesc(Employee::getId);
        return employeeMapper.selectList(w);
    }
}
