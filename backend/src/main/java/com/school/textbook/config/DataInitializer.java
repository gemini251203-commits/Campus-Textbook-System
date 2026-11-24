package com.school.textbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.textbook.entity.User;
import com.school.textbook.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 演示账号密码校验与初始化器，确保预置角色账号均可使用默认密码正常登录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_DEMO_PASSWORD = "admin123";
    private static final List<String> PRESET_USERNAMES = List.of(
        "admin", "teacher1", "student1", "staff1", "purchaser1", "teacher2"
    );

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String encodedPassword = passwordEncoder.encode(DEFAULT_DEMO_PASSWORD);
        int updatedCount = 0;
        for (String username : PRESET_USERNAMES) {
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (user != null && !passwordEncoder.matches(DEFAULT_DEMO_PASSWORD, user.getPassword())) {
                user.setPassword(encodedPassword);
                userMapper.updateById(user);
                updatedCount++;
            }
        }
        if (updatedCount > 0) {
            log.info("已完成 {} 个预置系统账号的 BCrypt 密码初始化校准", updatedCount);
        }
    }
}
