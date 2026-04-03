package com.example.fashonshop.security;

import com.example.fashonshop.entity.User;
import com.example.fashonshop.util.AppConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return buildToken(user, AppConstants.TOKEN_TYPE_ACCESS, accessTokenExpirationMs);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, AppConstants.TOKEN_TYPE_REFRESH, refreshTokenExpirationMs);
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserId(String token) {
        Object rawUserId = getClaims(token).get("userId");
        if (rawUserId instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(rawUserId));
    }

    public String getRole(String token) {
        return String.valueOf(getClaims(token).get("role"));
    }

    public String getTokenType(String token) {
        return String.valueOf(getClaims(token).get("tokenType"));
    }

    public boolean isAccessToken(String token) {
        return AppConstants.TOKEN_TYPE_ACCESS.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return AppConstants.TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    private String buildToken(User user, String tokenType, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .claim("tokenType", tokenType)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
