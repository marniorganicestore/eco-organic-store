package com.harvest.identity.web;

import java.util.List;

public final class AdminUserDtos {
    private AdminUserDtos() {}

    public record UpdateUserAccessRequest(List<String> roles, Boolean enabled) {}

    public record AdminUserResponse(
            String userId,
            String email,
            String name,
            String avatar,
            List<String> roles,
            boolean enabled) {}
}
