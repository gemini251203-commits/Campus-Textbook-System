package com.school.textbook.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 */
public class SecurityUtils {
    
    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserDetails) {
            return ((JwtUserDetails) authentication.getPrincipal()).getUserId();
        }
        return null;
    }
    
    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserDetails) {
            return ((JwtUserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }
    
    /**
     * 获取当前登录用户角色
     */
    public static String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserDetails) {
            return ((JwtUserDetails) authentication.getPrincipal()).getRole();
        }
        return null;
    }
}
