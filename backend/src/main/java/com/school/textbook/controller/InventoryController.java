package com.school.textbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.textbook.common.Result;
import com.school.textbook.entity.Inventory;
import com.school.textbook.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    /**
     * 分页查询库存
     */
    @GetMapping
    public Result<Page<Inventory>> listInventory(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean lowStock) {
        return Result.success(inventoryService.pageInventory(pageNum, pageSize, keyword, lowStock));
    }
    
    /**
     * 获取教材库存
     */
    @GetMapping("/book/{bookId}")
    public Result<Inventory> getByBookId(@PathVariable Long bookId) {
        Inventory inventory = inventoryService.getByBookId(bookId);
        return Result.success(inventory);
    }
    
    /**
     * 更新库存预警数量（工作人员/管理员）
     */
    @PutMapping("/{id}/warning")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<?> updateWarningQuantity(@PathVariable Long id, @RequestParam Integer warningQuantity) {
        Inventory inventory = new Inventory();
        inventory.setId(id);
        inventory.setWarningQuantity(warningQuantity);
        inventoryService.updateById(inventory);
        return Result.success("更新成功");
    }
    
    /**
     * 获取低库存预警列表
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PURCHASER')")
    public Result<List<Inventory>> getLowStockList() {
        return Result.success(inventoryService.getLowStockList());
    }
}
