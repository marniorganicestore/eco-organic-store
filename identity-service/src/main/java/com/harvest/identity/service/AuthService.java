package com.harvest.identity.service;

import com.harvest.common.security.JwtService;
import com.harvest.identity.domain.User;
import com.harvest.identity.repo.UserRepository;
import com.harvest.identity.web.AuthDtos.AuthResponse;
import com.harvest.identity.web.AuthDtos.ConfirmResetRequest;
import com.harvest.identity.web.AuthDtos.LoginRequest;
import com.harvest.identity.web.AuthDtos.MessageResponse;
import com.harvest.identity.web.AuthDtos.ProfileRequest;
import com.harvest.identity.web.AuthDtos.RequestResetRequest;
import com.harvest.identity.web.AuthDtos.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder encoder,
            JwtService jwtService,
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.cookie.same-site:Lax}") String cookieSameSite) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        userRepository.findByEmail(request.email().toLowerCase()).ifPresent(u -> {
            throw new IllegalArgumentException("Email already exists");
        });
        User user = new User();
        user.setEmail(request.email().toLowerCase());
        user.setName(request.name());
        user.setPasswordHash(encoder.encode(request.password()));
        user = userRepository.save(user);
        return issueTokens(user, response);
    }

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (user.getPasswordHash() == null || !encoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return issueTokens(user, response);
    }

    public AuthResponse googleLogin(String email, String name, String sub, HttpServletResponse response) {
        User user = userRepository.findByEmail(email.toLowerCase()).orElseGet(User::new);
        user.setEmail(email.toLowerCase());
        user.setName(name);
        user.setGoogleSub(sub);
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(List.of("CUSTOMER"));
        }
        user = userRepository.save(user);
        return issueTokens(user, response);
    }

    public AuthResponse refresh(String refreshToken, HttpServletResponse response) {
        var claims = jwtService.parse(refreshToken);
        User user = userRepository.findById(claims.getSubject()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        return issueTokens(user, response);
    }

    public void logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", refreshCookie("", Duration.ZERO).toString());
    }

    public User getMe(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User updateMe(String userId, ProfileRequest request) {
        User user = getMe(userId);
        user.setName(request.name());
        user.setAvatar(request.avatar());
        user.setAddresses(request.addresses());
        return userRepository.save(user);
    }

    public List<User> allUsers() {
        return userRepository.findAll();
    }

    public MessageResponse requestPasswordReset(RequestResetRequest request) {
        // Intentionally generic: never reveal whether email exists.
        userRepository.findByEmail(request.email().toLowerCase())
                .ifPresent(user -> log.info("Password reset requested for existing account userId={}", user.getId()));
        return new MessageResponse("If an account exists, password reset instructions will be sent.");
    }

    public MessageResponse confirmPasswordReset(ConfirmResetRequest request) {
        // Shell flow only in v1: token plumbing/email integration comes later.
        log.info("Password reset confirmation requested.");
        return new MessageResponse("Password reset request accepted.");
    }

    private AuthResponse issueTokens(User user, HttpServletResponse response) {
        String access = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRoles(), 900);
        String refresh = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRoles(), (int) REFRESH_TTL.toSeconds());
        response.addHeader("Set-Cookie", refreshCookie(refresh, REFRESH_TTL).toString());
        return new AuthResponse(access, user.getId(), user.getEmail(), user.getName(), user.getRoles());
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from("refreshToken", value)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();
    }
}