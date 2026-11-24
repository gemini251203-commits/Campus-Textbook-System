package com.school.textbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 缺书登记实体类
 */
@Data
@TableName("shortage")
public class Shortage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long bookId;
    
    private Integer requiredQuantity;
    
    private Integer currentQuantity;
    
    /**
     * 状态: 0待处理 1已发送采购 2已采购完成
     */
    private Integer status;
    
    private Long orderId;
    
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
}
