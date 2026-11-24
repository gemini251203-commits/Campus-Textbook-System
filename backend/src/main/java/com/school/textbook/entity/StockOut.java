package com.school.textbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 出库实体类
 */
@Data
@TableName("stock_out")
public class StockOut {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long bookId;
    
    private Integer quantity;
    
    private Long orderId;
    
    private Long operatorId;
    
    private String remark;
    
    private LocalDateTime createTime;
    
    // 非数据库字段
    @TableField(exist = false)
    private Book book;
    
    @TableField(exist = false)
    private BookOrder order;
    
    @TableField(exist = false)
    private User operator;
}
