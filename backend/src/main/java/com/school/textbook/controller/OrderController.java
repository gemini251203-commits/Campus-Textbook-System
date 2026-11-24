package com.school.textbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.textbook.common.Constants;
import com.school.textbook.common.Result;
import com.school.textbook.entity.BookOrder;
import com.school.textbook.service.BookOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final BookOrderService orderService;

    @Data
    public static class CreateOrderRequest {
        @NotNull(message = "教材ID不能为空")
        private Long bookId;
        @NotNull(message = "订购数量不能为空")
        @Min(value = 1, message = "订购数量至少为1册")
        private Integer quantity;
    }

    @Data
    public static class ReviewRequest {
        @NotNull(message = "审核结论不能为空")
        private Boolean approved;
        private String remark;
    }

    /**
     * 提交教材订购单
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER', 'ROLE_STUDENT')")
    public Result<BookOrder> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        BookOrder order = orderService.createOrder(request.getBookId(), request.getQuantity());
        return Result.success("订购单提交成功", order);
    }

    /**
     * 分页查询个人订购单
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER', 'ROLE_STUDENT')")
    public Result<Page<BookOrder>> getMyOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.success(orderService.getMyOrders(pageNum, pageSize, status, keyword));
    }

    /**
     * 分页查询全校订购单（管理员/库管员）
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<Page<BookOrder>> listOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword) {
        return Result.success(orderService.pageOrders(pageNum, pageSize, status, userId, keyword));
    }

    /**
     * 获取订单详情
     */
    @GetMapping("/{id}")
    public Result<BookOrder> getOrder(@PathVariable Long id) {
        return Result.success(orderService.getById(id));
    }

    /**
     * 审核订单（若库存不足自动转入缺书登记并精准提示）
     */
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<BookOrder> reviewOrder(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        BookOrder updated = orderService.reviewOrder(id, request.getApproved(), request.getRemark());
        String msg;
        if (!request.getApproved()) {
            msg = "订单已驳回";
        } else if (updated.getStatus() == Constants.ORDER_STATUS_PENDING) {
            msg = "当前库存不足，系统已自动生成缺书登记单并挂起该订单";
        } else {
            msg = "订单审核通过";
        }
        return Result.success(msg, updated);
    }

    /**
     * 结算开票登记
     */
    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<?> invoiceOrder(@PathVariable Long id) {
        orderService.invoiceOrder(id);
        return Result.success("结算开票登记成功", null);
    }

    /**
     * 书库拣货出库并转待领取
     */
    @PostMapping("/{id}/prepare-pickup")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<?> preparePickup(@PathVariable Long id) {
        orderService.preparePickup(id);
        return Result.success("书库出库完成，已通知师生领取", null);
    }

    /**
     * 确认领取完成
     */
    @PostMapping("/{id}/confirm-pickup")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<?> confirmPickup(@PathVariable Long id) {
        orderService.confirmPickup(id);
        return Result.success("已确认教材发放领取", null);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER', 'ROLE_STUDENT')")
    public Result<?> cancelOrder(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.success("订单已取消", null);
    }
}
