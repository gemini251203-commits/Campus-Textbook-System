package com.school.textbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订购实体类
 */
@Data
@TableName("book_order")
public class BookOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String orderNo;
    
    private Long userId;
    
    private Long bookId;
    
    private Integer quantity;
    
    private BigDecimal totalPrice;
    
    /**
     * 状态: 0待审核 1已审核 2已开票 3待领取 4已领取 5已取消
     */
    private Integer status;
    
    private Long reviewerId;
    
    private LocalDateTime reviewTime;
    
    private String reviewRemark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    // 非数据库字段
    @TableField(exist = false)
    private User user;
    
    @TableField(exist = false)
    private Book book;
    
    @TableField(exist = false)
    private User reviewer;
}
