package com.ecoorganicstore.identity.web;

import com.ecoorganicstore.identity.web.ProfileDtos.AddressResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 80) String name,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}
    public record LoginRequest(
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(max = 72) String password) {}
    public record GoogleRequest(@NotBlank String idToken) {}
    public record ChangePasswordRequest(
            @NotBlank(message = "Enter your current password.") @Size(max = 72) String currentPassword,
            @NotBlank(message = "Enter a new password.") @Size(min = 8, max = 72, message = "Password must be 8–72 characters.") String newPassword) {}
    public record AuthResponse(
            String accessToken, String userId, String email, String name, List<String> roles, String avatar) {}
    public record UserResponse(
            String userId, String email, String name, String avatar, List<String> roles, List<AddressResponse> addresses) {}
    public record RequestResetRequest(@NotBlank @Email @Size(max = 160) String email) {}
    public record ConfirmResetRequest(@NotBlank String token, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
    public record MessageResponse(String message) {}
}