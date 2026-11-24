package com.school.textbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.textbook.common.Result;
import com.school.textbook.entity.PendingPurchase;
import com.school.textbook.service.PendingPurchaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    
    private final PendingPurchaseService purchaseService;
    
    @Data
    public static class CreatePurchaseRequest {
        @NotNull(message = "教材ID不能为空")
        private Long bookId;
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate expectedDate;
        private String remark;
    }
    
    @Data
    public static class CreateFromShortageRequest {
        @NotNull(message = "缺书登记ID不能为空")
        private Long shortageId;
        private Integer quantity;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate expectedDate;
        private String remark;
    }
    
    @Data
    public static class CompletePurchaseRequest {
        private Integer actualQuantity;
        private String remark;
    }
    
    /**
     * 分页查询采购单
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PURCHASER')")
    public Result<Page<PendingPurchase>> listPurchases(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(purchaseService.pagePurchases(pageNum, pageSize, status));
    }
    
    /**
     * 获取采购单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PURCHASER')")
    public Result<PendingPurchase> getPurchase(@PathVariable Long id) {
        return Result.success(purchaseService.getById(id));
    }
    
    /**
     * 直接创建采购单（采购人员/管理员）
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PURCHASER')")
    public Result<?> createPurchase(@Valid @RequestBody CreatePurchaseRequest request) {
        purchaseService.createPurchase(
            request.getBookId(),
            request.getQuantity(),
            request.getExpectedDate(),
            request.getRemark()
        );
        return Result.success("采购单创建成功");
    }
    
    /**
     * 从缺书登记创建采购单
     */
    @PostMapping("/from-shortage")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PURCHASER')")
    public Result<?> createFromShortage(@Valid @RequestBody CreateFromShortageRequest request) {
        purchaseService.createFromShortage(
            request.getShortageId(),
            request.getQuantity(),
            request.getExpectedDate(),
            request.getRemark()
        );
        return Result.success("采购单创建成功");
    }
    
    /**
     * 开始采购（采购人员/管理员）
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PURCHASER')")
    public Result<?> startPurchase(@PathVariable Long id) {
        purchaseService.startPurchase(id);
        return Result.success("开始采购");
    }
    
    /**
     * 完成采购并入库（采购人员/管理员）
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PURCHASER')")
    public Result<?> completePurchase(@PathVariable Long id, 
                                       @RequestBody(required = false) CompletePurchaseRequest request) {
        Integer actualQuantity = request != null ? request.getActualQuantity() : null;
        String remark = request != null ? request.getRemark() : null;
        purchaseService.completePurchase(id, actualQuantity, remark);
        return Result.success("采购完成，已入库");
    }
}
