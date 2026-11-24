package com.school.textbook.common;

/**
 * 系统常量
 */
public class Constants {
    
    // 用户角色
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_PURCHASER = "PURCHASER";
    
    // 订单状态
    public static final int ORDER_STATUS_PENDING = 0;      // 待审核
    public static final int ORDER_STATUS_APPROVED = 1;     // 已审核
    public static final int ORDER_STATUS_INVOICED = 2;     // 已开票
    public static final int ORDER_STATUS_READY = 3;        // 待领取
    public static final int ORDER_STATUS_COMPLETED = 4;    // 已领取
    public static final int ORDER_STATUS_CANCELLED = 5;    // 已取消
    
    // 缺书登记状态
    public static final int SHORTAGE_STATUS_PENDING = 0;       // 待处理
    public static final int SHORTAGE_STATUS_SENT = 1;          // 已发送采购
    public static final int SHORTAGE_STATUS_COMPLETED = 2;     // 已采购完成
    
    // 采购状态
    public static final int PURCHASE_STATUS_PENDING = 0;       // 待采购
    public static final int PURCHASE_STATUS_PROCESSING = 1;    // 采购中
    public static final int PURCHASE_STATUS_COMPLETED = 2;     // 已入库
    
    // 用户状态
    public static final int USER_STATUS_DISABLED = 0;
    public static final int USER_STATUS_ENABLED = 1;
}
