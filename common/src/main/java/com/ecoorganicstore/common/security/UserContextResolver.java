package com.ecoorganicstore.common.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

public final class UserContextResolver {
    private UserContextResolver() {}

    public static UserContext fromHeaders(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String email = request.getHeader("X-User-Email");
        String rolesHeader = request.getHeader("X-User-Roles");
        List<String> roles = rolesHeader == null || rolesHeader.isBlank() ? List.of() : Arrays.stream(rolesHeader.split(",")).map(String::trim).toList();
        return new UserContext(userId, email, roles);
    }
}