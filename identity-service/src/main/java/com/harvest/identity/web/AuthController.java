package com.harvest.identity.web;

import com.harvest.common.security.AuthGuards;
import com.harvest.identity.service.AuthService;
import com.harvest.identity.service.GoogleIdTokenVerifierService;
import com.harvest.identity.web.AuthDtos.AuthResponse;
import com.harvest.identity.web.AuthDtos.ConfirmResetRequest;
import com.harvest.identity.web.AuthDtos.GoogleRequest;
import com.harvest.identity.web.AuthDtos.LoginRequest;
import com.harvest.identity.web.AuthDtos.MessageResponse;
import com.harvest.identity.web.AuthDtos.RegisterRequest;
import com.harvest.identity.web.AuthDtos.RequestResetRequest;
import com.harvest.identity.web.AuthDtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class AuthController {
    private final AuthService authService;
    private final GoogleIdTokenVerifierService googleIdTokenVerifierService;

    public AuthController(AuthService authService, GoogleIdTokenVerifierService googleIdTokenVerifierService) {
        this.authService = authService;
        this.googleIdTokenVerifierService = googleIdTokenVerifierService;
    }

    @PostMapping("/api/auth/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return authService.register(request, response);
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return authService.login(request, response);
    }

    @PostMapping("/api/auth/google")
    public AuthResponse google(@Valid @RequestBody GoogleRequest request, HttpServletResponse response) {
        var principal = googleIdTokenVerifierService.verify(request.idToken());
        return authService.googleLogin(
                principal.email(), principal.name(), principal.subject(), principal.picture(), response);
    }

    @PostMapping("/api/auth/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return authService.refresh(request, response);
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/admin/users")
    public List<UserResponse> users(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
        return authService.allUsers().stream().map(ProfileMapper::toUser).toList();
    }

    @PostMapping("/api/auth/request-reset")
    public MessageResponse requestReset(@Valid @RequestBody RequestResetRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/api/auth/confirm-reset")
    public MessageResponse confirmReset(@Valid @RequestBody ConfirmResetRequest request) {
        return authService.confirmPasswordReset(request);
    }
}