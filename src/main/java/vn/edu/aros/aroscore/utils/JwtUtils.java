package vn.edu.aros.aroscore.utils;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    private static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    // Khóa bí mật (Chuỗi này phải dài ít nhất 256 bit - dưới đây là mã Base64 ngẫu nhiên)
    private final String jwtSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Value("${jwt.access-expiration-ms:900000}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public String generateToken(String email) {
        return generateAccessToken(email);
    }

    public String generateAccessToken(String email) {
        return buildToken(email, jwtExpirationMs, TOKEN_TYPE_ACCESS);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, refreshExpirationMs, TOKEN_TYPE_REFRESH);
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpirationMs / 1000;
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        return parseClaimsSafely(authToken) != null;
    }

    public boolean validateAccessToken(String token) {
        Claims claims = parseClaimsSafely(token);
        if (claims == null) {
            return false;
        }
        String type = claims.get(CLAIM_TOKEN_TYPE, String.class);
        return type == null || TOKEN_TYPE_ACCESS.equals(type);
    }

    public boolean validateRefreshToken(String token) {
        Claims claims = parseClaimsSafely(token);
        return claims != null && TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    private String buildToken(String email, long expirationMs, String tokenType) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(email)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Claims parseClaimsSafely(String authToken) {
        try {
            return parseClaims(authToken);
        } catch (MalformedJwtException e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("JWT claims string is empty: " + e.getMessage());
        }
        return null;
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
