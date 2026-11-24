package com.school.textbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.textbook.entity.BookOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookOrderMapper extends BaseMapper<BookOrder> {
}
