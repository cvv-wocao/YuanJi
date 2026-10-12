package com.example.item.demo35.util;

import com.example.item.demo35.Dao.UserDao;
import com.example.item.demo35.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationMs;

    @Autowired
    private UserDao userDao;

    private Key key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // 生成 JWT Token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 从 Token 中提取用户名
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 从 Token 中提取用户ID
    public Integer getUserIdFromToken(String token) {
        try {
            String username = getUsernameFromToken(token);
            User user = userDao.selectByName(username);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // 验证 Token 是否有效
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 记录日志
            return false;
        }
    }
}
