package com.school.textbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.textbook.entity.Book;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
