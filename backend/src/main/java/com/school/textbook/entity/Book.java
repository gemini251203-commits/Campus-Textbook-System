package com.school.textbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 高校教材实体类
 */
@Data
@TableName("book")
public class Book {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String isbn;

    private String name;

    private String author;

    private String publisher;

    private BigDecimal price;

    private String category;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /**
     * 实时库存数量（非数据库持久化列）
     */
    @TableField(exist = false)
    private Integer stockQuantity;

    /**
     * 库存预警阈值（非数据库持久化列）
     */
    @TableField(exist = false)
    private Integer warningQuantity;
}
