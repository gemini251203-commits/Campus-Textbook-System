package com.school.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.textbook.common.BusinessException;
import com.school.textbook.entity.Book;
import com.school.textbook.entity.Inventory;
import com.school.textbook.mapper.BookMapper;
import com.school.textbook.mapper.InventoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class InventoryService extends ServiceImpl<InventoryMapper, Inventory> {

    private BookMapper bookMapper;

    @Autowired
    @Lazy
    public void setBookMapper(BookMapper bookMapper) {
        this.bookMapper = bookMapper;
    }

    /**
     * 根据教材 ID 获取库存记录
     */
    public Inventory getByBookId(Long bookId) {
        return lambdaQuery().eq(Inventory::getBookId, bookId).one();
    }

    /**
     * 分页查询库存台账
     */
    public Page<Inventory> pageInventory(int pageNum, int pageSize, String keyword, Boolean lowStock) {
        Page<Inventory> page = new Page<>(pageNum, pageSize);

        List<Long> bookIds = null;
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            LambdaQueryWrapper<Book> bookWrapper = new LambdaQueryWrapper<>();
            bookWrapper.like(Book::getName, kw)
                .or().like(Book::getIsbn, kw)
                .or().like(Book::getAuthor, kw)
                .or().like(Book::getPublisher, kw);
            bookIds = bookMapper.selectList(bookWrapper).stream().map(Book::getId).toList();
            if (bookIds.isEmpty()) {
                return page;
            }
        }

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<>();
        if (bookIds != null) {
            wrapper.in(Inventory::getBookId, bookIds);
        }

        if (Boolean.TRUE.equals(lowStock)) {
            wrapper.apply("quantity <= warning_quantity");
        }

        wrapper.orderByAsc(Inventory::getQuantity).orderByDesc(Inventory::getUpdateTime);
        Page<Inventory> result = page(page, wrapper);

        result.getRecords().forEach(inv -> inv.setBook(bookMapper.selectById(inv.getBookId())));

        return result;
    }

    /**
     * 增加库存
     */
    public void increaseStock(Long bookId, Integer quantity) {
        Inventory inventory = getByBookId(bookId);
        if (inventory == null) {
            Book book = bookMapper.selectById(bookId);
            if (book == null) {
                throw new BusinessException("关联教材不存在");
            }
            inventory = new Inventory();
            inventory.setBookId(bookId);
            inventory.setQuantity(quantity);
            inventory.setWarningQuantity(25);
            save(inventory);
            return;
        }
        inventory.setQuantity(inventory.getQuantity() + quantity);
        updateById(inventory);
    }

    /**
     * 扣减库存
     */
    public void decreaseStock(Long bookId, Integer quantity) {
        Inventory inventory = getByBookId(bookId);
        if (inventory == null) {
            throw new BusinessException("该教材尚无库存台账记录");
        }
        if (inventory.getQuantity() < quantity) {
            throw new BusinessException("当前在库数量不足，可用库存：" + inventory.getQuantity() + " 册");
        }
        inventory.setQuantity(inventory.getQuantity() - quantity);
        updateById(inventory);
    }

    /**
     * 校验库存是否满足需求
     */
    public boolean checkStock(Long bookId, Integer quantity) {
        Inventory inventory = getByBookId(bookId);
        return inventory != null && inventory.getQuantity() >= quantity;
    }

    /**
     * 查询所有低于或等于预警线的库存列表
     */
    public List<Inventory> getLowStockList() {
        List<Inventory> list = lambdaQuery()
            .apply("quantity <= warning_quantity")
            .orderByAsc(Inventory::getQuantity)
            .list();
        list.forEach(inv -> inv.setBook(bookMapper.selectById(inv.getBookId())));
        return list;
    }
}
