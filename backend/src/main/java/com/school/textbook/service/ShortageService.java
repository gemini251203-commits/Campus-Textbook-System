package com.school.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.textbook.common.BusinessException;
import com.school.textbook.common.Constants;
import com.school.textbook.entity.Inventory;
import com.school.textbook.entity.Shortage;
import com.school.textbook.mapper.BookMapper;
import com.school.textbook.mapper.ShortageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShortageService extends ServiceImpl<ShortageMapper, Shortage> {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private InventoryService inventoryService;

    /**
     * 创建或合并缺书登记记录
     */
    public Shortage createShortage(Long bookId, Integer requiredQuantity, Long orderId, String remark) {
        if (bookMapper.selectById(bookId) == null) {
            throw new BusinessException("关联教材不存在");
        }
        Inventory inventory = inventoryService.getByBookId(bookId);
        int currentQty = inventory != null ? inventory.getQuantity() : 0;

        Shortage existing = lambdaQuery()
            .eq(Shortage::getBookId, bookId)
            .eq(Shortage::getStatus, Constants.SHORTAGE_STATUS_PENDING)
            .one();

        if (existing != null) {
            existing.setRequiredQuantity(existing.getRequiredQuantity() + requiredQuantity);
            existing.setCurrentQuantity(currentQty);
            if (orderId != null && existing.getOrderId() == null) {
                existing.setOrderId(orderId);
            }
            if (StringUtils.hasText(remark)) {
                existing.setRemark(remark.trim());
            }
            updateById(existing);
            return existing;
        } else {
            Shortage shortage = new Shortage();
            shortage.setBookId(bookId);
            shortage.setRequiredQuantity(requiredQuantity);
            shortage.setCurrentQuantity(currentQty);
            shortage.setStatus(Constants.SHORTAGE_STATUS_PENDING);
            shortage.setOrderId(orderId);
            shortage.setDeleted(0);
            if (StringUtils.hasText(remark)) {
                shortage.setRemark(remark.trim());
            } else if (orderId != null) {
                shortage.setRemark("订单审核库存不足自动触发缺书登记");
            } else {
                shortage.setRemark("中心书库人工登记缺书补货");
            }
            save(shortage);
            return shortage;
        }
    }

    /**
     * 分页查询缺书登记列表
     */
    public Page<Shortage> pageShortages(int pageNum, int pageSize, Integer status) {
        Page<Shortage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Shortage> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(Shortage::getStatus, status);
        }
        wrapper.orderByAsc(Shortage::getStatus).orderByDesc(Shortage::getCreateTime);

        Page<Shortage> result = page(page, wrapper);
        result.getRecords().forEach(s -> s.setBook(bookMapper.selectById(s.getBookId())));

        return result;
    }

    /**
     * 标记已发送采购请求
     */
    public void sendToPurchase(Long shortageId) {
        Shortage shortage = getById(shortageId);
        if (shortage == null) {
            throw new BusinessException("缺书登记记录不存在");
        }
        if (shortage.getStatus() == Constants.SHORTAGE_STATUS_COMPLETED) {
            throw new BusinessException("该缺书登记已完成采购入库");
        }

        if (shortage.getStatus() == Constants.SHORTAGE_STATUS_PENDING) {
            shortage.setStatus(Constants.SHORTAGE_STATUS_SENT);
            updateById(shortage);
        }
    }

    /**
     * 标记采购入库已完成
     */
    public void markCompleted(Long shortageId) {
        Shortage shortage = getById(shortageId);
        if (shortage != null) {
            shortage.setStatus(Constants.SHORTAGE_STATUS_COMPLETED);
            updateById(shortage);
        }
    }
}
