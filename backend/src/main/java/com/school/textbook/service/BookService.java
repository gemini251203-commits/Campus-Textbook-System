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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookService extends ServiceImpl<BookMapper, Book> {

    private InventoryMapper inventoryMapper;

    @Autowired
    @Lazy
    public void setInventoryMapper(InventoryMapper inventoryMapper) {
        this.inventoryMapper = inventoryMapper;
    }

    /**
     * 分页查询教材并关联实时库存数量
     */
    public Page<Book> pageBooks(int pageNum, int pageSize, String keyword, String category) {
        Page<Book> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Book::getName, kw)
                .or().like(Book::getAuthor, kw)
                .or().like(Book::getIsbn, kw)
                .or().like(Book::getPublisher, kw));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Book::getCategory, category.trim());
        }
        wrapper.orderByAsc(Book::getId);

        Page<Book> result = page(page, wrapper);
        fillBookStock(result.getRecords());
        return result;
    }

    /**
     * 获取单本教材详情（含库存余量）
     */
    public Book getBookDetail(Long id) {
        Book book = getById(id);
        if (book != null) {
            fillBookStock(List.of(book));
        }
        return book;
    }

    private void fillBookStock(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return;
        }
        List<Long> bookIds = books.stream().map(Book::getId).toList();
        List<Inventory> inventories = inventoryMapper.selectList(
            new LambdaQueryWrapper<Inventory>().in(Inventory::getBookId, bookIds)
        );
        Map<Long, Inventory> invMap = inventories.stream()
            .collect(Collectors.toMap(Inventory::getBookId, Function.identity(), (a, b) -> a));

        for (Book book : books) {
            Inventory inv = invMap.get(book.getId());
            book.setStockQuantity(inv != null ? inv.getQuantity() : 0);
            book.setWarningQuantity(inv != null ? inv.getWarningQuantity() : 25);
        }
    }

    /**
     * 创建教材并同步初始化库存台账
     */
    @Transactional
    public void createBook(Book book, Integer initialQuantity, Integer warningQuantity) {
        if (!StringUtils.hasText(book.getName())) {
            throw new BusinessException("教材名称不能为空");
        }
        if (StringUtils.hasText(book.getIsbn())) {
            String isbn = book.getIsbn().trim();
            Book existing = lambdaQuery().eq(Book::getIsbn, isbn).one();
            if (existing != null) {
                throw new BusinessException("该 ISBN 编号已存在");
            }
            book.setIsbn(isbn);
        }
        if (book.getPrice() == null) {
            book.setPrice(BigDecimal.ZERO);
        }
        book.setDeleted(0);
        save(book);

        Inventory inventory = new Inventory();
        inventory.setBookId(book.getId());
        inventory.setQuantity(initialQuantity != null && initialQuantity >= 0 ? initialQuantity : 0);
        inventory.setWarningQuantity(warningQuantity != null && warningQuantity > 0 ? warningQuantity : 25);
        inventoryMapper.insert(inventory);
    }

    /**
     * 更新教材基础信息
     */
    public void updateBook(Book book) {
        Book existing = getById(book.getId());
        if (existing == null) {
            throw new BusinessException("教材不存在");
        }

        if (StringUtils.hasText(book.getIsbn()) && !book.getIsbn().trim().equals(existing.getIsbn())) {
            Book byIsbn = lambdaQuery().eq(Book::getIsbn, book.getIsbn().trim()).one();
            if (byIsbn != null) {
                throw new BusinessException("该 ISBN 编号已被其他教材占用");
            }
            book.setIsbn(book.getIsbn().trim());
        }

        updateById(book);
    }

    /**
     * 获取所有非空教材分类
     */
    public List<String> getAllCategories() {
        return listObjs(
            lambdaQuery().select(Book::getCategory).isNotNull(Book::getCategory).getWrapper(),
            obj -> obj != null ? obj.toString() : ""
        ).stream()
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
    }
}
