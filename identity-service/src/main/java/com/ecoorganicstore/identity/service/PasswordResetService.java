package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.identity.domain.PasswordResetToken;
import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.PasswordResetTokenRepository;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.service.mail.AccountMail;
import com.ecoorganicstore.identity.service.mail.PasswordResets;
import com.ecoorganicstore.identity.service.mail.StoreMessages;
import com.ecoorganicstore.identity.web.AuthDtos.ConfirmResetRequest;
import com.ecoorganicstore.identity.web.AuthDtos.MessageResponse;
import com.ecoorganicstore.identity.web.AuthDtos.RequestResetRequest;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService implements PasswordResets {
    static final Duration TTL = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final AccountMail mail;
    private final StoreMessages messages;

    public PasswordResetService(
            UserRepository users,
            PasswordResetTokenRepository tokens,
            PasswordEncoder encoder,
            AccountMail mail,
            StoreMessages messages) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mail = mail;
        this.messages = messages;
    }

    @Override
    public MessageResponse request(RequestResetRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        users.findByEmail(email).ifPresent(this::issue);
        return new MessageResponse("If an account exists, password reset instructions will be sent.");
    }

    @Override
    public MessageResponse confirm(ConfirmResetRequest request) {
        PasswordResetToken token = tokens.findByTokenHash(hash(request.token().trim()))
                .filter(saved -> saved.getExpiresAt() != null && saved.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has expired."));
        User user = users.findById(token.getUserId())
                .filter(User::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has expired."));
        user.setPasswordHash(encoder.encode(request.newPassword()));
        user.setRefreshTokenVersion(user.getRefreshTokenVersion() + 1);
        users.save(user);
        tokens.delete(token);
        mail.passwordChanged(display(user), user.getEmail());
        return new MessageResponse("Password updated. Sign in with your new password.");
    }

    private void issue(User user) {
        if (!user.isEnabled()) {
            return;
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            mail.googleSignIn(display(user), user.getEmail());
            return;
        }
        tokens.deleteByUserId(user.getId());
        String raw = newToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(TTL));
        tokens.save(token);
        mail.passwordReset(display(user), user.getEmail(), messages.storefrontUrl() + "/forgot-password?token=" + raw);
    }

    private static String display(User user) {
        return user.getName() == null || user.getName().isBlank() ? user.getEmail() : user.getName();
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
