package com.school.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.textbook.common.BusinessException;
import com.school.textbook.entity.StockIn;
import com.school.textbook.mapper.*;
import com.school.textbook.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class StockInService extends ServiceImpl<StockInMapper, StockIn> {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PendingPurchaseMapper pendingPurchaseMapper;

    private String generateBatchNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
        return "IN" + ts + rand;
    }

    /**
     * 登记教材入库并同步增加库存
     */
    @Transactional
    public void stockIn(Long bookId, Integer quantity, Long purchaseId, String remark) {
        if (bookMapper.selectById(bookId) == null) {
            throw new BusinessException("所选教材不存在");
        }
        StockIn stockIn = new StockIn();
        stockIn.setBookId(bookId);
        stockIn.setQuantity(quantity);
        stockIn.setPurchaseId(purchaseId);
        stockIn.setOperatorId(SecurityUtils.getCurrentUserId());
        stockIn.setBatchNo(generateBatchNo());
        stockIn.setRemark(StringUtils.hasText(remark) ? remark.trim() : "中心书库手动登记入库");
        stockIn.setCreateTime(LocalDateTime.now());

        save(stockIn);

        inventoryService.increaseStock(bookId, quantity);
    }

    /**
     * 分页查询入库流水
     */
    public Page<StockIn> pageStockIn(int pageNum, int pageSize, Long bookId) {
        Page<StockIn> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();

        if (bookId != null) {
            wrapper.eq(StockIn::getBookId, bookId);
        }
        wrapper.orderByDesc(StockIn::getCreateTime);

        Page<StockIn> result = page(page, wrapper);

        result.getRecords().forEach(s -> {
            s.setBook(bookMapper.selectById(s.getBookId()));
            s.setOperator(userMapper.selectById(s.getOperatorId()));
            if (s.getPurchaseId() != null) {
                s.setPurchase(pendingPurchaseMapper.selectById(s.getPurchaseId()));
            }
        });

        return result;
    }
}
