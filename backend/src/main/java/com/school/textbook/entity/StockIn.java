package com.school.textbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 进库实体类
 */
@Data
@TableName("stock_in")
public class StockIn {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long bookId;
    
    private Integer quantity;
    
    private Long purchaseId;
    
    private Long operatorId;
    
    private String batchNo;
    
    private String remark;
    
    private LocalDateTime createTime;
    
    // 非数据库字段
    @TableField(exist = false)
    private Book book;
    
    @TableField(exist = false)
    private PendingPurchase purchase;
    
    @TableField(exist = false)
    private User operator;
}
