package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.identity.domain.PasswordResetToken;
import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.PasswordResetTokenRepository;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.service.mail.AccountMail;
import com.ecoorganicstore.identity.service.mail.StoreMessages;
import com.ecoorganicstore.identity.web.AuthDtos.ConfirmResetRequest;
import com.ecoorganicstore.identity.web.AuthDtos.RequestResetRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {
    private final StoreMessages messages = new StoreMessages(
            "Marni eco organic store", "admin@eco-organic-store.com", "admin@eco-organic-store.com", "http://localhost:5173");

    @Test
    void unknownEmailDoesNotSendAndStaysGeneric() {
        UserRepository users = mock(UserRepository.class);
        AccountMail mail = mock(AccountMail.class);
        when(users.findByEmail("missing@eco-organic-store.com")).thenReturn(Optional.empty());
        PasswordResetService resets = service(users, tokenRepo(new HashMap<>()), mail, mock(PasswordEncoder.class));

        var message = resets.request(new RequestResetRequest("missing@eco-organic-store.com"));

        assertEquals("If an account exists, password reset instructions will be sent.", message.message());
        verify(mail, never()).passwordReset(any(), any(), any());
        verify(mail, never()).googleSignIn(any(), any());
    }

    @Test
    void resetLinkIsSingleUseAndUpdatesThePassword() {
        UserRepository users = mock(UserRepository.class);
        Map<String, PasswordResetToken> stored = new HashMap<>();
        CapturedMail mail = new CapturedMail();
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User user = customer();
        user.setPasswordHash("old-hash");
        when(users.findByEmail("ada@eco-organic-store.com")).thenReturn(Optional.of(user));
        when(users.findById("u-1")).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode("new-password")).thenReturn("new-hash");
        PasswordResetService resets = service(users, tokenRepo(stored), mail, encoder);

        var requested = resets.request(new RequestResetRequest("Ada@eco-organic-store.com"));
        assertEquals("If an account exists, password reset instructions will be sent.", requested.message());
        assertTrue(mail.resetUrl.startsWith("http://localhost:5173/forgot-password?token="));
        String token = mail.resetUrl.substring(mail.resetUrl.indexOf("token=") + 6);

        var confirmed = resets.confirm(new ConfirmResetRequest(token, "new-password"));

        assertEquals("Password updated. Sign in with your new password.", confirmed.message());
        assertEquals("new-hash", user.getPasswordHash());
        assertEquals(1, user.getRefreshTokenVersion());
        assertEquals("ada@eco-organic-store.com", mail.changedEmail);
        assertThrows(IllegalArgumentException.class, () -> resets.confirm(new ConfirmResetRequest(token, "new-password")));
    }

    @Test
    void expiredOrUnknownTokenIsRejected() {
        Map<String, PasswordResetToken> stored = new HashMap<>();
        PasswordResetToken expired = new PasswordResetToken();
        expired.setUserId("u-1");
        expired.setTokenHash(PasswordResetService.hash("stale-token"));
        expired.setExpiresAt(java.time.Instant.now().minusSeconds(5));
        stored.put(expired.getTokenHash(), expired);
        PasswordResetService resets = service(mock(UserRepository.class), tokenRepo(stored), mock(AccountMail.class), mock(PasswordEncoder.class));

        IllegalArgumentException expiredLink = assertThrows(
                IllegalArgumentException.class,
                () -> resets.confirm(new ConfirmResetRequest("stale-token", "new-password")));
        IllegalArgumentException unknown = assertThrows(
                IllegalArgumentException.class,
                () -> resets.confirm(new ConfirmResetRequest("missing-token", "new-password")));

        assertEquals("This reset link is invalid or has expired.", expiredLink.getMessage());
        assertEquals("This reset link is invalid or has expired.", unknown.getMessage());
    }

    @Test
    void googleAccountGetsASignInNoteInsteadOfAResetLink() {
        UserRepository users = mock(UserRepository.class);
        AccountMail mail = mock(AccountMail.class);
        User user = customer();
        when(users.findByEmail("ada@eco-organic-store.com")).thenReturn(Optional.of(user));
        PasswordResetService resets = service(users, tokenRepo(new HashMap<>()), mail, mock(PasswordEncoder.class));

        resets.request(new RequestResetRequest("ada@eco-organic-store.com"));

        verify(mail).googleSignIn("Ada", "ada@eco-organic-store.com");
        verify(mail, never()).passwordReset(any(), any(), any());
    }

    private PasswordResetService service(
            UserRepository users, PasswordResetTokenRepository tokens, AccountMail mail, PasswordEncoder encoder) {
        return new PasswordResetService(users, tokens, encoder, mail, messages);
    }

    private static PasswordResetTokenRepository tokenRepo(Map<String, PasswordResetToken> stored) {
        PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        when(tokens.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            stored.put(token.getTokenHash(), token);
            return token;
        });
        when(tokens.findByTokenHash(any())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            stored.remove(token.getTokenHash());
            return null;
        }).when(tokens).delete(any(PasswordResetToken.class));
        return tokens;
    }

    private static User customer() {
        User user = new User();
        user.setId("u-1");
        user.setEmail("ada@eco-organic-store.com");
        user.setName("Ada");
        user.setEnabled(true);
        return user;
    }

    private static final class CapturedMail implements AccountMail {
        private String resetUrl = "";
        private String changedEmail = "";

        @Override public void welcome(String name, String email) {}
        @Override public void passwordReset(String name, String email, String resetUrl) { this.resetUrl = resetUrl; }
        @Override public void passwordChanged(String name, String email) { this.changedEmail = email; }
        @Override public void googleSignIn(String name, String email) {}
    }
}
