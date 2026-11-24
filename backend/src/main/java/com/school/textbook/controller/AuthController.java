package com.school.textbook.controller;

import com.school.textbook.common.BusinessException;
import com.school.textbook.common.Constants;
import com.school.textbook.common.Result;
import com.school.textbook.entity.User;
import com.school.textbook.security.JwtTokenProvider;
import com.school.textbook.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Set<String> REGISTERABLE_ROLES = Set.of(
        Constants.ROLE_TEACHER,
        Constants.ROLE_STUDENT
    );

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String realName;
        @NotBlank(message = "角色不能为空")
        private String role;
        private String phone;
        private String email;
    }

    /**
     * 用户登录签发 JWT 令牌
     */
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != Constants.USER_STATUS_ENABLED) {
            throw new BusinessException("该账号已被停用，请联系教务管理员");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        return Result.success(data);
    }

    /**
     * 师生自助注册（仅开放教师与学生角色）
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        if (!REGISTERABLE_ROLES.contains(request.getRole())) {
            throw new BusinessException("自助注册仅支持教师或学生身份");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setRole(request.getRole());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());

        userService.createUser(user);

        return Result.success("注册成功", null);
    }
}
