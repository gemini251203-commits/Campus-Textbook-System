package com.school.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.textbook.common.BusinessException;
import com.school.textbook.entity.StockOut;
import com.school.textbook.mapper.BookMapper;
import com.school.textbook.mapper.BookOrderMapper;
import com.school.textbook.mapper.StockOutMapper;
import com.school.textbook.mapper.UserMapper;
import com.school.textbook.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class StockOutService extends ServiceImpl<StockOutMapper, StockOut> {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BookOrderMapper bookOrderMapper;

    /**
     * 登记教材出库并扣减库存
     */
    @Transactional
    public void stockOut(Long bookId, Integer quantity, Long orderId, String remark) {
        if (bookMapper.selectById(bookId) == null) {
            throw new BusinessException("所选教材不存在");
        }
        inventoryService.decreaseStock(bookId, quantity);

        StockOut stockOut = new StockOut();
        stockOut.setBookId(bookId);
        stockOut.setQuantity(quantity);
        stockOut.setOrderId(orderId);
        stockOut.setOperatorId(SecurityUtils.getCurrentUserId());
        stockOut.setRemark(StringUtils.hasText(remark) ? remark.trim() : "中心书库手动调拨出库");
        stockOut.setCreateTime(LocalDateTime.now());

        save(stockOut);
    }

    /**
     * 分页查询出库流水
     */
    public Page<StockOut> pageStockOut(int pageNum, int pageSize, Long bookId) {
        Page<StockOut> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<StockOut> wrapper = new LambdaQueryWrapper<>();

        if (bookId != null) {
            wrapper.eq(StockOut::getBookId, bookId);
        }
        wrapper.orderByDesc(StockOut::getCreateTime);

        Page<StockOut> result = page(page, wrapper);

        result.getRecords().forEach(s -> {
            s.setBook(bookMapper.selectById(s.getBookId()));
            s.setOperator(userMapper.selectById(s.getOperatorId()));
            if (s.getOrderId() != null) {
                s.setOrder(bookOrderMapper.selectById(s.getOrderId()));
            }
        });

        return result;
    }
}
