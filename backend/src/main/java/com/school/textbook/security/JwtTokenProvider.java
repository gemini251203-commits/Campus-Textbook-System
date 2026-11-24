package com.school.textbook.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;

/**
 * JWT Token提供者
 */
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;
    
    private SecretKey key;
    
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成Token
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
            .subject(username)
            .claim("userId", userId)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }
    
    /**
     * 从Token获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.getSubject();
    }
    
    /**
     * 从Token获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.get("userId", Long.class);
    }
    
    /**
     * 从Token获取角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.get("role", String.class);
    }
    
    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 获取认证信息
     */
    public Authentication getAuthentication(String token) {
        String username = getUsernameFromToken(token);
        String role = getRoleFromToken(token);
        Long userId = getUserIdFromToken(token);
        
        JwtUserDetails userDetails = new JwtUserDetails(userId, username, role);
        
        return new UsernamePasswordAuthenticationToken(
            userDetails, 
            null, 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
