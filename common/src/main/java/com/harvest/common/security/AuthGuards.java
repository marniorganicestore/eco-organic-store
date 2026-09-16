package com.harvest.common.security;

import com.harvest.common.web.ForbiddenException;
import com.harvest.common.web.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthGuards {
    private AuthGuards() {}

    public static UserContext requireUser(HttpServletRequest request) {
        UserContext ctx = UserContextResolver.fromHeaders(request);
        if (ctx.userId() == null || ctx.userId().isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
        return ctx;
    }

    public static UserContext requireAdmin(HttpServletRequest request) {
        UserContext ctx = requireUser(request);
        if (!ctx.isAdmin()) {
            throw new ForbiddenException("Admin access required");
        }
        return ctx;
    }
}
