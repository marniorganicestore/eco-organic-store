package com.harvest.gateway.filter;

import com.harvest.common.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtRelayFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtRelayFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Map<String, String> extra = new HashMap<>();
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parse(auth.substring(7));
                if (jwtService.isUsableAsAccessToken(claims)) {
                    extra.put("X-User-Id", claims.getSubject());
                    extra.put("X-User-Email", String.valueOf(claims.get("email")));
                    Object roles = claims.get("roles");
                    if (roles instanceof List<?> roleList) {
                        extra.put("X-User-Roles", String.join(",", roleList.stream().map(String::valueOf).toList()));
                    }
                }
            } catch (Exception ignored) {
                // Forward as guest when token is invalid or is a refresh JWT.
            }
        }
        HttpServletRequest wrapped = new HeaderMapRequestWrapper(request, extra);
        filterChain.doFilter(wrapped, response);
    }

    private static final class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> custom;

        private HeaderMapRequestWrapper(HttpServletRequest request, Map<String, String> custom) {
            super(request);
            this.custom = custom;
        }

        @Override
        public String getHeader(String name) {
            if (name != null && name.toLowerCase().startsWith("x-user-")) {
                return getCustomHeader(name);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (name != null && name.toLowerCase().startsWith("x-user-")) {
                String value = getCustomHeader(name);
                return value == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames()).stream()
                    .filter(n -> !n.toLowerCase().startsWith("x-user-"))
                    .toList();
            List<String> mutable = new java.util.ArrayList<>(names);
            custom.keySet().forEach(k -> {
                if (custom.get(k) != null && !mutable.contains(k)) {
                    mutable.add(k);
                }
            });
            return Collections.enumeration(mutable);
        }

        private String getCustomHeader(String name) {
            return custom.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(name))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
    }
}