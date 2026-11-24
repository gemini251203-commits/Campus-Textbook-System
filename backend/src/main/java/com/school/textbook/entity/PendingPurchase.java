package com.school.textbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待购教材实体类
 */
@Data
@TableName("pending_purchase")
public class PendingPurchase {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long shortageId;
    
    private Long bookId;
    
    private Integer quantity;
    
    /**
     * 状态: 0待采购 1采购中 2已入库
     */
    private Integer status;
    
    private Long purchaserId;
    
    private LocalDate expectedDate;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    // 非数据库字段
    @TableField(exist = false)
    private Book book;
    
    @TableField(exist = false)
    private Shortage shortage;
    
    @TableField(exist = false)
    private User purchaser;
}
