package com.example.mt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mt.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
