package com.school.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.textbook.common.BusinessException;
import com.school.textbook.common.Constants;
import com.school.textbook.entity.BookOrder;
import com.school.textbook.entity.PendingPurchase;
import com.school.textbook.entity.Shortage;
import com.school.textbook.entity.StockIn;
import com.school.textbook.mapper.*;
import com.school.textbook.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PendingPurchaseService extends ServiceImpl<PendingPurchaseMapper, PendingPurchase> {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private ShortageService shortageService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StockInMapper stockInMapper;

    @Autowired
    private BookOrderMapper bookOrderMapper;

    @Autowired
    private InventoryService inventoryService;

    /**
     * 由缺书登记单转化生成采购单
     */
    @Transactional
    public PendingPurchase createFromShortage(Long shortageId, Integer quantity, LocalDate expectedDate, String remark) {
        Shortage shortage = shortageService.getById(shortageId);
        if (shortage == null) {
            throw new BusinessException("缺书登记记录不存在");
        }
        if (shortage.getStatus() == Constants.SHORTAGE_STATUS_COMPLETED) {
            throw new BusinessException("该缺书记录已完成采购入库，无需重复生成采购单");
        }

        int targetQty = (quantity != null && quantity > 0)
            ? quantity
            : Math.max(1, shortage.getRequiredQuantity() - (shortage.getCurrentQuantity() != null ? shortage.getCurrentQuantity() : 0));

        PendingPurchase purchase = new PendingPurchase();
        purchase.setShortageId(shortageId);
        purchase.setBookId(shortage.getBookId());
        purchase.setQuantity(targetQty);
        purchase.setStatus(Constants.PURCHASE_STATUS_PENDING);
        purchase.setExpectedDate(expectedDate != null ? expectedDate : LocalDate.now().plusDays(7));
        purchase.setRemark(StringUtils.hasText(remark) ? remark.trim() : "依据缺书登记单生成的教材补货计划");
        purchase.setDeleted(0);

        save(purchase);

        shortageService.sendToPurchase(shortageId);
        return purchase;
    }

    /**
     * 直接新建采购计划单
     */
    public PendingPurchase createPurchase(Long bookId, Integer quantity, LocalDate expectedDate, String remark) {
        if (bookMapper.selectById(bookId) == null) {
            throw new BusinessException("所选教材不存在");
        }
        PendingPurchase purchase = new PendingPurchase();
        purchase.setBookId(bookId);
        purchase.setQuantity(quantity);
        purchase.setStatus(Constants.PURCHASE_STATUS_PENDING);
        purchase.setExpectedDate(expectedDate != null ? expectedDate : LocalDate.now().plusDays(7));
        purchase.setRemark(StringUtils.hasText(remark) ? remark.trim() : "常规教材库存补充采购");
        purchase.setDeleted(0);

        save(purchase);
        return purchase;
    }

    /**
     * 分页查询采购计划单
     */
    public Page<PendingPurchase> pagePurchases(int pageNum, int pageSize, Integer status) {
        Page<PendingPurchase> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PendingPurchase> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(PendingPurchase::getStatus, status);
        }
        wrapper.orderByAsc(PendingPurchase::getStatus).orderByDesc(PendingPurchase::getCreateTime);

        Page<PendingPurchase> result = page(page, wrapper);

        result.getRecords().forEach(p -> {
            p.setBook(bookMapper.selectById(p.getBookId()));
            if (p.getShortageId() != null) {
                p.setShortage(shortageService.getById(p.getShortageId()));
            }
            if (p.getPurchaserId() != null) {
                p.setPurchaser(userMapper.selectById(p.getPurchaserId()));
            }
        });

        return result;
    }

    /**
     * 启动采购执行
     */
    public void startPurchase(Long purchaseId) {
        PendingPurchase purchase = getById(purchaseId);
        if (purchase == null) {
            throw new BusinessException("采购单不存在");
        }
        if (purchase.getStatus() != Constants.PURCHASE_STATUS_PENDING) {
            throw new BusinessException("仅待采购状态的单据可执行开始采购");
        }

        purchase.setStatus(Constants.PURCHASE_STATUS_PROCESSING);
        purchase.setPurchaserId(SecurityUtils.getCurrentUserId());
        updateById(purchase);
    }

    private String generateBatchNo() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
        return "IN" + ts + rand;
    }

    /**
     * 采购到货验收入库，并联动更新库存、缺书单及关联挂起订单说明
     */
    @Transactional
    public void completePurchase(Long purchaseId, Integer actualQuantity, String remark) {
        PendingPurchase purchase = getById(purchaseId);
        if (purchase == null) {
            throw new BusinessException("采购单不存在");
        }
        if (purchase.getStatus() == Constants.PURCHASE_STATUS_COMPLETED) {
            throw new BusinessException("该采购单已完成验收入库");
        }

        int qty = (actualQuantity != null && actualQuantity > 0) ? actualQuantity : purchase.getQuantity();
        Long operatorId = SecurityUtils.getCurrentUserId();

        StockIn stockIn = new StockIn();
        stockIn.setBookId(purchase.getBookId());
        stockIn.setQuantity(qty);
        stockIn.setPurchaseId(purchaseId);
        stockIn.setOperatorId(operatorId);
        stockIn.setBatchNo(generateBatchNo());
        stockIn.setRemark(StringUtils.hasText(remark) ? remark.trim() : "采购单到货验收入库");
        stockIn.setCreateTime(LocalDateTime.now());
        stockInMapper.insert(stockIn);

        inventoryService.increaseStock(purchase.getBookId(), qty);

        if (purchase.getPurchaserId() == null) {
            purchase.setPurchaserId(operatorId);
        }
        purchase.setStatus(Constants.PURCHASE_STATUS_COMPLETED);
        updateById(purchase);

        if (purchase.getShortageId() != null) {
            Shortage shortage = shortageService.getById(purchase.getShortageId());
            shortageService.markCompleted(purchase.getShortageId());
            if (shortage != null && shortage.getOrderId() != null) {
                BookOrder order = bookOrderMapper.selectById(shortage.getOrderId());
                if (order != null && order.getStatus() == Constants.ORDER_STATUS_PENDING) {
                    order.setReviewRemark("缺书教材已采购入库，可重新审核发放");
                    bookOrderMapper.updateById(order);
                }
            }
        }
    }
}
