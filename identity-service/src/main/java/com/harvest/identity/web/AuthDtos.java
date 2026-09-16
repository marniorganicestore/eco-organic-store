package com.harvest.identity.web;

import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(String name, String email, String password) {}
    public record LoginRequest(String email, String password) {}
    public record GoogleRequest(String idToken) {}
    public record ProfileRequest(String name, String avatar, List<String> addresses) {}
    public record AuthResponse(String accessToken, String userId, String email, String name, List<String> roles) {}
    public record UserResponse(String userId, String email, String name, String avatar, List<String> roles, List<String> addresses) {}
}