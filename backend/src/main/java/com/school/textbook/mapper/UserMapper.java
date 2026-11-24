package com.school.textbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.textbook.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
