package com.team4.service;

import com.team4.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import com.team4.util.BusinessException;

/**
 * Xử lý tạo và xác thực JSON Web Tokens (JWT).
 */
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    // Chuỗi bí mật (Cần đưa vào biến môi trường trong thực tế)
    private static final String SECRET_STRING = "VGhpcyBJcyBBIFZlcnkgU2VjdXJlIEFuZCBMb25nIFNlY3JldCBLZXkgRm9yIEpXVCBBdXRoZW50aWNhdGlvbg==";

    private static final Key SECRET_KEY = new SecretKeySpec(Base64.getDecoder().decode(SECRET_STRING),
            SignatureAlgorithm.HS256.getJcaName());

    // Thời gian hết hạn: 24 giờ
    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000;

    /**
     * Tạo JWT từ thông tin người dùng.
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Xác thực và trích xuất Claims từ JWT.
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            logger.warn("Expired JWT token: {}", ex.getMessage());
            throw new BusinessException("Expired JWT token");
        } catch (SignatureException ex) {
            logger.error("Invalid JWT signature. Potential tampering attack.", ex);
            throw new BusinessException("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.warn("Malformed JWT token: {}", ex.getMessage());
            throw new BusinessException("Malformed JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.warn("Unsupported JWT token: {}", ex.getMessage());
            throw new BusinessException("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.warn("JWT claims string is empty.", ex);
            throw new BusinessException("JWT claims string is empty");
        }
    }
}
