package com.school.textbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.textbook.common.Result;
import com.school.textbook.entity.Book;
import com.school.textbook.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Data
    public static class CreateBookRequest {
        private String isbn;
        @NotBlank(message = "教材名称不能为空")
        private String name;
        private String author;
        private String publisher;
        @DecimalMin(value = "0.00", message = "价格不能为负数")
        private BigDecimal price;
        private String category;
        private String description;
        private Integer initialQuantity;
        private Integer warningQuantity;
    }

    /**
     * 分页查询教材目录
     */
    @GetMapping
    public Result<Page<Book>> listBooks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return Result.success(bookService.pageBooks(pageNum, pageSize, keyword, category));
    }

    /**
     * 获取教材详情
     */
    @GetMapping("/{id}")
    public Result<Book> getBook(@PathVariable Long id) {
        return Result.success(bookService.getBookDetail(id));
    }

    /**
     * 新增教材并建立库存台账
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<?> createBook(@Valid @RequestBody CreateBookRequest request) {
        Book book = new Book();
        book.setIsbn(request.getIsbn());
        book.setName(request.getName());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPrice(request.getPrice());
        book.setCategory(request.getCategory());
        book.setDescription(request.getDescription());

        bookService.createBook(book, request.getInitialQuantity(), request.getWarningQuantity());
        return Result.success("教材录入成功", null);
    }

    /**
     * 更新教材信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
    public Result<?> updateBook(@PathVariable Long id, @RequestBody Book book) {
        book.setId(id);
        bookService.updateBook(book);
        return Result.success("教材信息已更新", null);
    }

    /**
     * 删除教材（管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<?> deleteBook(@PathVariable Long id) {
        bookService.removeById(id);
        return Result.success("教材已删除", null);
    }

    /**
     * 获取所有教材学科分类
     */
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(bookService.getAllCategories());
    }
}
