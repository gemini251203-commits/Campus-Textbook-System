package com.school.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.textbook.common.BusinessException;
import com.school.textbook.common.Constants;
import com.school.textbook.entity.*;
import com.school.textbook.mapper.*;
import com.school.textbook.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BookOrderService extends ServiceImpl<BookOrderMapper, BookOrder> {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ShortageService shortageService;

    @Autowired
    private StockOutMapper stockOutMapper;

    /**
     * 生成唯一订单编号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return "ORD" + timestamp + random;
    }

    /**
     * 创建教材订购单
     */
    @Transactional
    public BookOrder createOrder(Long bookId, Integer quantity) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException("所选教材不存在");
        }

        BigDecimal unitPrice = book.getPrice() != null ? book.getPrice() : BigDecimal.ZERO;

        BookOrder order = new BookOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(SecurityUtils.getCurrentUserId());
        order.setBookId(bookId);
        order.setQuantity(quantity);
        order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        order.setStatus(Constants.ORDER_STATUS_PENDING);
        order.setDeleted(0);

        save(order);
        return order;
    }

    /**
     * 分页查询订购单
     */
    public Page<BookOrder> pageOrders(int pageNum, int pageSize, Integer status, Long userId, String keyword) {
        Page<BookOrder> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BookOrder> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(BookOrder::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(BookOrder::getUserId, userId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> matchedBookIds = bookMapper.selectList(
                new LambdaQueryWrapper<Book>().like(Book::getName, kw).or().like(Book::getIsbn, kw)
            ).stream().map(Book::getId).toList();

            wrapper.and(w -> {
                w.like(BookOrder::getOrderNo, kw);
                if (!matchedBookIds.isEmpty()) {
                    w.or().in(BookOrder::getBookId, matchedBookIds);
                }
            });
        }
        wrapper.orderByDesc(BookOrder::getCreateTime);

        Page<BookOrder> result = page(page, wrapper);

        result.getRecords().forEach(order -> {
            order.setUser(userMapper.selectById(order.getUserId()));
            order.setBook(bookMapper.selectById(order.getBookId()));
            if (order.getReviewerId() != null) {
                order.setReviewer(userMapper.selectById(order.getReviewerId()));
            }
        });

        return result;
    }

    /**
     * 获取当前登录用户的订购单
     */
    public Page<BookOrder> getMyOrders(int pageNum, int pageSize, Integer status, String keyword) {
        return pageOrders(pageNum, pageSize, status, SecurityUtils.getCurrentUserId(), keyword);
    }

    /**
     * 审核订单（若库存不足则自动触发缺书挂起，保留审核说明）
     */
    @Transactional
    public BookOrder reviewOrder(Long orderId, boolean approved, String remark) {
        BookOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != Constants.ORDER_STATUS_PENDING) {
            throw new BusinessException("当前订单状态不可执行审核");
        }

        String cleanRemark = StringUtils.hasText(remark) ? remark.trim() : null;

        if (approved) {
            if (!inventoryService.checkStock(order.getBookId(), order.getQuantity())) {
                String shortageNote = "关联订单 " + order.getOrderNo() + " 库存不足自动挂起";
                shortageService.createShortage(order.getBookId(), order.getQuantity(), orderId, shortageNote);
                order.setStatus(Constants.ORDER_STATUS_PENDING);
                order.setReviewRemark(cleanRemark != null
                    ? "库存不足，已登记缺书挂起（" + cleanRemark + "）"
                    : "库存不足，已自动登记缺书申请并挂起订单");
            } else {
                order.setStatus(Constants.ORDER_STATUS_APPROVED);
                order.setReviewRemark(cleanRemark != null ? cleanRemark : "库存核验充足，审核通过");
            }
        } else {
            order.setStatus(Constants.ORDER_STATUS_CANCELLED);
            order.setReviewRemark(cleanRemark != null ? cleanRemark : "审核未通过，订单已驳回");
        }

        order.setReviewerId(SecurityUtils.getCurrentUserId());
        order.setReviewTime(LocalDateTime.now());
        updateById(order);
        return order;
    }

    /**
     * 财务结算开票登记
     */
    public void invoiceOrder(Long orderId) {
        BookOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != Constants.ORDER_STATUS_APPROVED) {
            throw new BusinessException("仅已审核订单可执行开票登记");
        }

        order.setStatus(Constants.ORDER_STATUS_INVOICED);
        updateById(order);
    }

    /**
     * 书库拣货出库并登记待领取
     */
    @Transactional
    public void preparePickup(Long orderId) {
        BookOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != Constants.ORDER_STATUS_INVOICED) {
            throw new BusinessException("仅已开票订单可执行书库出库");
        }

        inventoryService.decreaseStock(order.getBookId(), order.getQuantity());

        StockOut stockOut = new StockOut();
        stockOut.setBookId(order.getBookId());
        stockOut.setQuantity(order.getQuantity());
        stockOut.setOrderId(orderId);
        stockOut.setOperatorId(SecurityUtils.getCurrentUserId());
        stockOut.setRemark("订单出库：" + order.getOrderNo());
        stockOut.setCreateTime(LocalDateTime.now());
        stockOutMapper.insert(stockOut);

        order.setStatus(Constants.ORDER_STATUS_READY);
        updateById(order);
    }

    /**
     * 确认师生签收领取
     */
    public void confirmPickup(Long orderId) {
        BookOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != Constants.ORDER_STATUS_READY) {
            throw new BusinessException("仅待领取订单可确认发放完成");
        }

        order.setStatus(Constants.ORDER_STATUS_COMPLETED);
        updateById(order);
    }

    /**
     * 取消订单（支持本人或教务管理员在出库前取消）
     */
    public void cancelOrder(Long orderId) {
        BookOrder order = getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() > Constants.ORDER_STATUS_APPROVED) {
            throw new BusinessException("订单已进入开票或出库环节，无法取消");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentRole = SecurityUtils.getCurrentUserRole();
        boolean isOwner = order.getUserId() != null && order.getUserId().equals(currentUserId);
        boolean isAdmin = Constants.ROLE_ADMIN.equals(currentRole);

        if (!isOwner && !isAdmin) {
            throw new BusinessException("无权取消他人订单");
        }

        order.setStatus(Constants.ORDER_STATUS_CANCELLED);
        if (!StringUtils.hasText(order.getReviewRemark())) {
            order.setReviewRemark(isAdmin && !isOwner ? "管理员取消订单" : "订购人主动取消订单");
        }
        updateById(order);
    }
}
