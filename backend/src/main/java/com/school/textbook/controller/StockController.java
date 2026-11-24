package com.school.textbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.textbook.common.Result;
import com.school.textbook.entity.StockIn;
import com.school.textbook.entity.StockOut;
import com.school.textbook.service.StockInService;
import com.school.textbook.service.StockOutService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {
    
    private final StockInService stockInService;
    private final StockOutService stockOutService;
    
    @Data
    public static class StockInRequest {
        @NotNull(message = "教材ID不能为空")
        private Long bookId;
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
        private Long purchaseId;
        private String remark;
    }
    
    @Data
    public static class StockOutRequest {
        @NotNull(message = "教材ID不能为空")
        private Long bookId;
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
        private Long orderId;
        private String remark;
    }
    
    /**
     * 分页查询入库记录
     */
    @GetMapping("/in")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PURCHASER')")
    public Result<Page<StockIn>> listStockIn(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long bookId) {
        return Result.success(stockInService.pageStockIn(pageNum, pageSize, bookId));
    }
    
    /**
     * 手动入库（采购人员/管理员）
     */
    @PostMapping("/in")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_PURCHASER')")
    public Result<?> stockIn(@Valid @RequestBody StockInRequest request) {
        stockInService.stockIn(
            request.getBookId(),
            request.getQuantity(),
            request.getPurchaseId(),
            request.getRemark()
        );
        return Result.success("入库成功");
    }
    
    /**
     * 分页查询出库记录
     */
    @GetMapping("/out")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<Page<StockOut>> listStockOut(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long bookId) {
        return Result.success(stockOutService.pageStockOut(pageNum, pageSize, bookId));
    }
    
    /**
     * 手动出库（工作人员/管理员）
     */
    @PostMapping("/out")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<?> stockOut(@Valid @RequestBody StockOutRequest request) {
        stockOutService.stockOut(
            request.getBookId(),
            request.getQuantity(),
            request.getOrderId(),
            request.getRemark()
        );
        return Result.success("出库成功");
    }
}
