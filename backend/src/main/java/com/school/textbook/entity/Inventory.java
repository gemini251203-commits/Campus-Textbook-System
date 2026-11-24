package com.school.textbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 库存实体类
 */
@Data
@TableName("inventory")
public class Inventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long bookId;
    
    private Integer quantity;
    
    private Integer warningQuantity;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    // 非数据库字段，用于关联查询
    @TableField(exist = false)
    private Book book;
}
