package com.example.mt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mt.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
