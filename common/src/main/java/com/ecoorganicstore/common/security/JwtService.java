package com.ecoorganicstore.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

public class JwtService {
    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_REFRESH_VERSION = "rv";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;

    public JwtService(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userId, String email, List<String> roles, long ttlSeconds) {
        return createToken(userId, email, roles, ttlSeconds, TYPE_ACCESS, 0);
    }

    public String createRefreshToken(
            String userId, String email, List<String> roles, long ttlSeconds, int refreshVersion) {
        return createToken(userId, email, roles, ttlSeconds, TYPE_REFRESH, refreshVersion);
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public boolean isUsableAsAccessToken(Claims claims) {
        String type = tokenType(claims);
        return type == null || TYPE_ACCESS.equals(type);
    }

    public boolean isUsableAsRefreshToken(Claims claims) {
        String type = tokenType(claims);
        return type == null || TYPE_REFRESH.equals(type);
    }

    public int refreshVersion(Claims claims) {
        Object value = claims.get(CLAIM_REFRESH_VERSION);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private String tokenType(Claims claims) {
        Object type = claims.get(CLAIM_TYPE);
        return type == null ? null : String.valueOf(type);
    }

    private String createToken(
            String userId, String email, List<String> roles, long ttlSeconds, String type, int refreshVersion) {
        Instant now = Instant.now();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("email", email);
        claims.put("roles", roles);
        claims.put(CLAIM_TYPE, type);
        if (TYPE_REFRESH.equals(type)) {
            claims.put(CLAIM_REFRESH_VERSION, refreshVersion);
        }
        return Jwts.builder()
                .subject(userId)
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(secretKey)
                .compact();
    }
}
