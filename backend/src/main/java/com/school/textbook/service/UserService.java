package com.school.textbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.textbook.common.BusinessException;
import com.school.textbook.entity.User;
import com.school.textbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;

    /**
     * 根据用户名查询用户
     */
    public User getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return lambdaQuery().eq(User::getUsername, username.trim()).one();
    }

    /**
     * 创建用户
     */
    public void createUser(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        if (getByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setUsername(user.getUsername().trim());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setDeleted(0);
        save(user);
    }

    /**
     * 分页查询用户
     */
    public Page<User> pageUsers(int pageNum, int pageSize, String keyword, String role) {
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, kw)
                .or().like(User::getRealName, kw)
                .or().like(User::getPhone, kw)
                .or().like(User::getEmail, kw));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }
        wrapper.orderByAsc(User::getId);

        return page(page, wrapper);
    }

    /**
     * 更新用户信息
     */
    public void updateUser(User user) {
        User existing = getById(user.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }

        if (StringUtils.hasText(user.getUsername()) && !existing.getUsername().equals(user.getUsername().trim())) {
            if (getByUsername(user.getUsername()) != null) {
                throw new BusinessException("用户名已存在");
            }
            user.setUsername(user.getUsername().trim());
        } else {
            user.setUsername(existing.getUsername());
        }

        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }

        updateById(user);
    }

    /**
     * 修改登录密码
     */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }
}
