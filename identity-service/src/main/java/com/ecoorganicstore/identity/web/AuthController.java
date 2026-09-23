package com.ecoorganicstore.identity.web;

import com.ecoorganicstore.identity.service.AuthService;
import com.ecoorganicstore.identity.service.GoogleIdTokenVerifierService;
import com.ecoorganicstore.identity.web.AuthDtos.AuthResponse;
import com.ecoorganicstore.identity.web.AuthDtos.ConfirmResetRequest;
import com.ecoorganicstore.identity.web.AuthDtos.GoogleRequest;
import com.ecoorganicstore.identity.web.AuthDtos.LoginRequest;
import com.ecoorganicstore.identity.web.AuthDtos.MessageResponse;
import com.ecoorganicstore.identity.web.AuthDtos.RegisterRequest;
import com.ecoorganicstore.identity.web.AuthDtos.RequestResetRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/api/auth/request-reset")
    public MessageResponse requestReset(@Valid @RequestBody RequestResetRequest request) {
        return authService.requestPasswordReset(request);
    }

    @PostMapping("/api/auth/confirm-reset")
    public MessageResponse confirmReset(@Valid @RequestBody ConfirmResetRequest request) {
        return authService.confirmPasswordReset(request);
    }
}