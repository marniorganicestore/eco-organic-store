package com.harvest.identity.service;

import com.harvest.common.security.JwtService;
import com.harvest.common.web.UnauthorizedException;
import com.harvest.identity.domain.User;
import com.harvest.identity.repo.UserRepository;
import com.harvest.identity.web.AuthDtos.AuthResponse;
import com.harvest.identity.web.AuthDtos.ConfirmResetRequest;
import com.harvest.identity.web.AuthDtos.LoginRequest;
import com.harvest.identity.web.AuthDtos.MessageResponse;
import com.harvest.identity.web.AuthDtos.ProfileRequest;
import com.harvest.identity.web.AuthDtos.RequestResetRequest;
import com.harvest.identity.web.AuthDtos.RegisterRequest;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    static final String REFRESH_COOKIE = "refreshToken";
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private volatile String cachedDummyPasswordHash;

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
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            encoder.matches(request.password(), dummyPasswordHash());
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return issueTokens(user, response);
    }

    public AuthResponse googleLogin(String email, String name, String sub, String picture, HttpServletResponse response) {
        User user = userRepository.findByEmail(email.toLowerCase()).orElseGet(User::new);
        user.setEmail(email.toLowerCase());
        if (name != null && !name.isBlank()) {
            user.setName(name);
        } else if (user.getName() == null || user.getName().isBlank()) {
            user.setName(email);
        }
        user.setGoogleSub(sub);
        if (picture != null && !picture.isBlank()) {
            user.setAvatar(picture);
        }
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(List.of("CUSTOMER"));
        }
        user = userRepository.save(user);
        return issueTokens(user, response);
    }

    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readCookie(request, REFRESH_COOKIE);
        if (refreshToken == null) {
            throw new UnauthorizedException("Missing refresh token");
        }
        User user = requireRefreshSession(refreshToken);
        return issueTokens(user, response);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        User user = resolveLogoutUser(request);
        if (user != null) {
            user.setRefreshTokenVersion(user.getRefreshTokenVersion() + 1);
            userRepository.save(user);
        }
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

    static String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private User requireRefreshSession(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (RuntimeException ex) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (!jwtService.isUsableAsRefreshToken(claims)) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        User user = userRepository.findById(claims.getSubject())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (jwtService.refreshVersion(claims) != user.getRefreshTokenVersion()) {
            throw new UnauthorizedException("Session expired");
        }
        return user;
    }

    private User resolveLogoutUser(HttpServletRequest request) {
        String refresh = readCookie(request, REFRESH_COOKIE);
        if (refresh != null) {
            try {
                return requireRefreshSession(refresh);
            } catch (RuntimeException ignored) {
                // Fall through to the access-token identity the gateway already verified.
            }
        }
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private AuthResponse issueTokens(User user, HttpServletResponse response) {
        String access = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRoles(), 900);
        String refresh = jwtService.createRefreshToken(
                user.getId(), user.getEmail(), user.getRoles(), (int) REFRESH_TTL.toSeconds(), user.getRefreshTokenVersion());
        response.addHeader("Set-Cookie", refreshCookie(refresh, REFRESH_TTL).toString());
        return new AuthResponse(access, user.getId(), user.getEmail(), user.getName(), user.getRoles(), user.getAvatar());
    }

    private String dummyPasswordHash() {
        String hash = cachedDummyPasswordHash;
        if (hash == null) {
            hash = encoder.encode("harvest-not-a-user");
            cachedDummyPasswordHash = hash;
        }
        return hash;
    }

    private ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .build();
    }
}
