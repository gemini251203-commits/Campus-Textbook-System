package com.school.textbook.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.textbook.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {
}
