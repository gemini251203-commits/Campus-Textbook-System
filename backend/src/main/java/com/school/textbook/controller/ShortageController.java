package com.school.textbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.textbook.common.BusinessException;
import com.school.textbook.common.Result;
import com.school.textbook.entity.Shortage;
import com.school.textbook.service.ShortageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shortages")
@RequiredArgsConstructor
public class ShortageController {

    private final ShortageService shortageService;

    @Data
    public static class CreateShortageRequest {
        private Long bookId;
        private Integer requiredQuantity;
        private String remark;
    }

    /**
     * 分页查询缺书登记记录
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PURCHASER')")
    public Result<Page<Shortage>> listShortages(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.success(shortageService.pageShortages(pageNum, pageSize, status));
    }

    /**
     * 获取缺书登记详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PURCHASER')")
    public Result<Shortage> getShortage(@PathVariable Long id) {
        return Result.success(shortageService.getById(id));
    }

    /**
     * 手动登记缺书记录（兼容 URL 参数与 JSON 请求体）
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PURCHASER')")
    public Result<Shortage> createShortage(
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Integer requiredQuantity,
            @RequestParam(required = false) String remark,
            @RequestBody(required = false) CreateShortageRequest body) {
        Long finalBookId = bookId != null ? bookId : (body != null ? body.getBookId() : null);
        Integer finalQty = requiredQuantity != null ? requiredQuantity : (body != null ? body.getRequiredQuantity() : null);
        String finalRemark = remark != null ? remark : (body != null ? body.getRemark() : null);

        if (finalBookId == null || finalQty == null || finalQty < 1) {
            throw new BusinessException("请选择教材并填写有效的缺书需求数量");
        }
        Shortage created = shortageService.createShortage(finalBookId, finalQty, null, finalRemark);
        return Result.success("缺书登记成功", created);
    }

    /**
     * 推送缺书需求至采购环节
     */
    @PostMapping("/{id}/send-purchase")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF', 'ROLE_PURCHASER')")
    public Result<?> sendToPurchase(@PathVariable Long id) {
        shortageService.sendToPurchase(id);
        return Result.success("已推送采购申请", null);
    }
}
