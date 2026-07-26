package com.example.mt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mt.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}
