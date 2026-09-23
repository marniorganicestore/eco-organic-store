package com.ecoorganicstore.common.security;

import io.jsonwebtoken.Claims;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {
    private static final String SECRET = "change-me-please-change-me-please-change-me-please";
    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void accessTokenIsNotUsableForRefresh() {
        String token = jwtService.createAccessToken("u-1", "user@eco-organic-store.com", List.of("CUSTOMER"), 600);
        Claims claims = jwtService.parse(token);

        assertTrue(jwtService.isUsableAsAccessToken(claims));
        assertFalse(jwtService.isUsableAsRefreshToken(claims));
        assertEquals(0, jwtService.refreshVersion(claims));
    }

    @Test
    void refreshTokenCarriesVersionAndIsNotUsableAsBearer() {
        String token = jwtService.createRefreshToken("u-1", "user@eco-organic-store.com", List.of("CUSTOMER"), 3600, 7);
        Claims claims = jwtService.parse(token);

        assertFalse(jwtService.isUsableAsAccessToken(claims));
        assertTrue(jwtService.isUsableAsRefreshToken(claims));
        assertEquals(7, jwtService.refreshVersion(claims));
        assertEquals("u-1", claims.getSubject());
    }
}
