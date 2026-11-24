package com.school.textbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.textbook.common.BusinessException;
import com.school.textbook.common.Result;
import com.school.textbook.entity.User;
import com.school.textbook.security.SecurityUtils;
import com.school.textbook.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Result<User> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("未登录或凭证已失效");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return Result.unauthorized("当前账号不存在");
        }
        return Result.success(user);
    }

    /**
     * 分页查询系统用户（教务管理员）
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<Page<User>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        return Result.success(userService.pageUsers(pageNum, pageSize, keyword, role));
    }

    /**
     * 新增系统用户（教务管理员）
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<?> createUser(@Valid @RequestBody User user) {
        userService.createUser(user);
        return Result.success("用户创建成功", null);
    }

    /**
     * 更新系统用户（教务管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.success("用户信息已更新", null);
    }

    /**
     * 删除系统用户（保护内置管理员与自身账号）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<?> deleteUser(@PathVariable Long id) {
        User target = userService.getById(id);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if ("admin".equalsIgnoreCase(target.getUsername())) {
            throw new BusinessException("系统内置管理员账号不可删除");
        }
        if (id.equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException("不能删除当前正在登录的账号");
        }
        userService.removeById(id);
        return Result.success("用户已删除", null);
    }

    /**
     * 修改当前登录用户密码
     */
    @PostMapping("/change-password")
    public Result<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(
            SecurityUtils.getCurrentUserId(),
            request.getOldPassword(),
            request.getNewPassword()
        );
        return Result.success("密码修改成功", null);
    }

    /**
     * 启用或停用账号状态（教务管理员）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User target = userService.getById(id);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }
        if ("admin".equalsIgnoreCase(target.getUsername()) && status == 0) {
            throw new BusinessException("系统内置管理员账号不可停用");
        }
        if (id.equals(SecurityUtils.getCurrentUserId()) && status == 0) {
            throw new BusinessException("不能停用当前正在登录的账号");
        }
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userService.updateById(user);
        return Result.success("账号状态已更新", null);
    }
}
