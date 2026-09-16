package com.harvest.gateway.filter;

import com.harvest.common.security.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtRelayFilterTest {
    private static final String SECRET = "change-me-please-change-me-please-change-me-please";

    @Test
    void stripsSpoofedUserHeadersWithoutToken() throws ServletException, IOException {
        JwtRelayFilter filter = new JwtRelayFilter(new JwtService(SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("x-user-id", "spoofed");
        request.setRequestURI("/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<HttpServletRequest> captured = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> captured.set((HttpServletRequest) req));

        assertNull(captured.get().getHeader("X-User-Id"));
        assertNull(captured.get().getHeader("x-user-id"));
    }

    @Test
    void usesJwtHeadersOverSpoofedHeaders() throws ServletException, IOException {
        JwtService jwtService = new JwtService(SECRET);
        String jwt = jwtService.createAccessToken("real-user", "real@harvest.co", List.of("CUSTOMER"), 600);
        JwtRelayFilter filter = new JwtRelayFilter(jwtService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        request.addHeader("x-user-id", "spoofed");
        request.setRequestURI("/api/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<HttpServletRequest> captured = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> captured.set((HttpServletRequest) req));

        assertEquals("real-user", captured.get().getHeader("X-User-Id"));
    }
}
