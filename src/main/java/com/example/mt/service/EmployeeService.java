package com.example.mt.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.mt.entity.Employee;

public interface EmployeeService {

    /** 员工分页查询（租户隔离 + Redis 缓存提速） */
    IPage<Employee> page(String keyword, long page, long size);

    Long add(Employee emp);

    void update(Employee emp);

    void delete(Long id);
}
