package com.harvest.common.security;

import java.util.List;

public record UserContext(String userId, String email, List<String> roles) {
    public boolean isAdmin() {
        return roles != null && roles.contains(Roles.ADMIN);
    }
}