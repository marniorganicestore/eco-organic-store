package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.common.security.JwtService;
import com.ecoorganicstore.common.web.UnauthorizedException;
import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.service.mail.AccountMail;
import com.ecoorganicstore.identity.service.mail.PasswordResets;
import com.ecoorganicstore.identity.web.AuthDtos.ChangePasswordRequest;
import com.ecoorganicstore.identity.web.AuthDtos.LoginRequest;
import com.ecoorganicstore.identity.web.AuthDtos.MessageResponse;
import com.ecoorganicstore.identity.web.AuthDtos.RegisterRequest;
import com.ecoorganicstore.identity.web.AuthDtos.RequestResetRequest;
import org.mockito.ArgumentCaptor;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private static final String SECRET = "change-me-please-change-me-please-change-me-please";

    @Test
    void loginReturnsAccessTokenAndSetsRefreshCookie() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));

        User user = customer("u-1", 0);
        user.setPasswordHash("hash");

        when(userRepository.findByEmail("user@eco-organic-store.com")).thenReturn(Optional.of(user));
        when(encoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.createAccessToken(eq("u-1"), eq("user@eco-organic-store.com"), anyList(), anyLong()))
                .thenReturn("jwt-token");
        when(jwtService.createRefreshToken(eq("u-1"), eq("user@eco-organic-store.com"), anyList(), anyLong(), eq(0)))
                .thenReturn("refresh-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        var result = authService.login(new LoginRequest("user@eco-organic-store.com", "secret123"), response);

        assertEquals("jwt-token", result.accessToken());
        assertEquals("user@eco-organic-store.com", result.email());
        assertTrue(response.getHeader("Set-Cookie").contains("refreshToken=refresh-token"));
        assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    void loginRejectsWrongPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));

        User user = new User();
        user.setEmail("user@eco-organic-store.com");
        user.setPasswordHash("hash");
        when(userRepository.findByEmail("user@eco-organic-store.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrongpass", "hash")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new LoginRequest("user@eco-organic-store.com", "wrongpass"), new MockHttpServletResponse()));
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void loginRejectsUnknownEmailWithSameMessage() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));

        when(userRepository.findByEmail("missing@eco-organic-store.com")).thenReturn(Optional.empty());
        when(encoder.encode("unknown-user")).thenReturn("dummy-hash");
        when(encoder.matches("secret123", "dummy-hash")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new LoginRequest("missing@eco-organic-store.com", "secret123"), new MockHttpServletResponse()));
        assertEquals("Invalid email or password", exception.getMessage());
        verify(encoder).matches("secret123", "dummy-hash");
    }

    @Test
    void googleLoginStoresAvatarAndIssuesTokens() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));

        when(userRepository.findByEmail("user@eco-organic-store.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("u-1");
            return saved;
        });
        when(jwtService.createAccessToken(eq("u-1"), eq("user@eco-organic-store.com"), anyList(), anyLong())).thenReturn("jwt-token");
        when(jwtService.createRefreshToken(eq("u-1"), eq("user@eco-organic-store.com"), anyList(), anyLong(), eq(0)))
                .thenReturn("refresh-token");

        var result = authService.googleLogin(
                "user@eco-organic-store.com", "User", "sub-1", "https://img.test/a.png", new MockHttpServletResponse());

        assertEquals("jwt-token", result.accessToken());
        assertEquals("https://img.test/a.png", result.avatar());
    }

    @Test
    void googleLoginKeepsAPhotoTheCustomerChose() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));
        User existing = new User();
        existing.setId("u-1");
        existing.setEmail("user@eco-organic-store.com");
        existing.setName("User");
        existing.setAvatar("/api/avatars/11111111-1111-1111-1111-111111111111");
        existing.setAvatarChosen(true);
        existing.setRoles(List.of("CUSTOMER"));
        when(userRepository.findByEmail("user@eco-organic-store.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.createAccessToken(eq("u-1"), eq("user@eco-organic-store.com"), anyList(), anyLong())).thenReturn("jwt-token");
        when(jwtService.createRefreshToken(eq("u-1"), eq("user@eco-organic-store.com"), anyList(), anyLong(), eq(0)))
                .thenReturn("refresh-token");

        var result = authService.googleLogin(
                "user@eco-organic-store.com", "User", "sub-1", "https://img.test/google.png", new MockHttpServletResponse());

        assertEquals("/api/avatars/11111111-1111-1111-1111-111111111111", result.avatar());
    }

    @Test
    void logoutRevokesRefreshFamilyAndClearsCookie() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = new JwtService(SECRET);
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));

        User user = customer("u-1", 3);
        String refresh = jwtService.createRefreshToken("u-1", "user@eco-organic-store.com", List.of("CUSTOMER"), 3600, 3);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthService.REFRESH_COOKIE, refresh));
        MockHttpServletResponse response = new MockHttpServletResponse();

        authService.logout(request, response);

        assertEquals(4, user.getRefreshTokenVersion());
        verify(userRepository).save(user);
        String setCookie = response.getHeader("Set-Cookie");
        assertTrue(setCookie.contains("refreshToken="));
        assertTrue(setCookie.contains("Max-Age=0"));
        assertTrue(setCookie.contains("HttpOnly"));
    }

    @Test
    void logoutRevokesViaGatewayUserWhenCookieIsMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthService authService = new AuthService(
                userRepository, mock(PasswordEncoder.class), new JwtService(SECRET), false, "Lax", AccountMail.none(), mock(PasswordResets.class));

        User user = customer("u-1", 1);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "u-1");

        authService.logout(request, new MockHttpServletResponse());

        assertEquals(2, user.getRefreshTokenVersion());
        verify(userRepository).save(user);
    }

    @Test
    void refreshRejectsRevokedRefreshToken() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = new JwtService(SECRET);
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));

        User user = customer("u-1", 5);
        String staleRefresh = jwtService.createRefreshToken("u-1", "user@eco-organic-store.com", List.of("CUSTOMER"), 3600, 4);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthService.REFRESH_COOKIE, staleRefresh));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.refresh(request, new MockHttpServletResponse()));
        assertEquals("Session expired", exception.getMessage());
    }

    @Test
    void refreshRejectsAccessToken() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = new JwtService(SECRET);
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));

        String access = jwtService.createAccessToken("u-1", "user@eco-organic-store.com", List.of("CUSTOMER"), 600);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthService.REFRESH_COOKIE, access));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.refresh(request, new MockHttpServletResponse()));
        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void refreshRejectsMissingCookie() {
        AuthService authService = new AuthService(
                mock(UserRepository.class), mock(PasswordEncoder.class), new JwtService(SECRET), false, "Lax", AccountMail.none(), mock(PasswordResets.class));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.refresh(new MockHttpServletRequest(), new MockHttpServletResponse()));
        assertEquals("Missing refresh token", exception.getMessage());
    }

    @Test
    void changePasswordRotatesTheSessionWhenTheCurrentPasswordMatches() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));
        User user = customer("u-1", 2);
        user.setPasswordHash("hash");
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.matches("current-password", "hash")).thenReturn(true);
        when(encoder.matches("new-password-1", "hash")).thenReturn(false);
        when(encoder.encode("new-password-1")).thenReturn("new-hash");
        when(jwtService.createAccessToken(eq("u-1"), eq("user@eco-organic-store.com"), anyList(), anyLong())).thenReturn("next-access");
        when(jwtService.createRefreshToken(eq("u-1"), eq("user@eco-organic-store.com"), anyList(), anyLong(), eq(3))).thenReturn("next-refresh");

        var response = new MockHttpServletResponse();
        var result = authService.changePassword("u-1", new ChangePasswordRequest("current-password", "new-password-1"), response);

        assertEquals("next-access", result.accessToken());
        assertEquals("new-hash", user.getPasswordHash());
        assertEquals(3, user.getRefreshTokenVersion());
        assertTrue(response.getHeader("Set-Cookie").contains("refreshToken=next-refresh"));
    }

    @Test
    void changePasswordRejectsGoogleAccountsAndAWrongCurrentPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(userRepository, encoder, mock(JwtService.class), false, "Lax", AccountMail.none(), mock(PasswordResets.class));
        User googleUser = customer("u-1", 0);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(googleUser));

        IllegalArgumentException googleOnly = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword("u-1", new ChangePasswordRequest("current-password", "new-password-1"), new MockHttpServletResponse()));
        assertEquals("This account signs in with Google and does not have a password.", googleOnly.getMessage());

        googleUser.setPasswordHash("hash");
        when(encoder.matches("wrong-password", "hash")).thenReturn(false);
        IllegalArgumentException wrongCurrent = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword("u-1", new ChangePasswordRequest("wrong-password", "new-password-1"), new MockHttpServletResponse()));
        assertEquals("Current password is incorrect.", wrongCurrent.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesAnEnabledCustomerAndRejectsADuplicateEmail() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));
        when(userRepository.findByEmail("ada@eco-organic-store.com")).thenReturn(Optional.empty());
        when(encoder.encode("secret123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("u-new");
            return saved;
        });
        when(jwtService.createAccessToken(eq("u-new"), eq("ada@eco-organic-store.com"), eq(List.of("CUSTOMER")), anyLong()))
                .thenReturn("jwt-token");
        when(jwtService.createRefreshToken(eq("u-new"), eq("ada@eco-organic-store.com"), eq(List.of("CUSTOMER")), anyLong(), eq(0)))
                .thenReturn("refresh-token");

        var result = authService.register(
                new RegisterRequest("  Ada   Lovelace  ", "Ada@eco-organic-store.com", "secret123"), new MockHttpServletResponse());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(List.of("CUSTOMER"), saved.getValue().getRoles());
        assertTrue(saved.getValue().isEnabled());
        assertEquals("Ada Lovelace", result.name());
        assertEquals("ada@eco-organic-store.com", result.email());
        assertEquals(List.of("CUSTOMER"), result.roles());

        when(userRepository.findByEmail("ada@eco-organic-store.com")).thenReturn(Optional.of(saved.getValue()));
        IllegalArgumentException duplicate = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(
                        new RegisterRequest("Ada Lovelace", "ada@eco-organic-store.com", "secret123"), new MockHttpServletResponse()));
        assertEquals("An account with this email already exists.", duplicate.getMessage());
    }

    @Test
    void loginRejectsADisabledAccountOnlyAfterThePasswordMatches() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AuthService authService = new AuthService(userRepository, encoder, mock(JwtService.class), false, "Lax", AccountMail.none(), mock(PasswordResets.class));
        User user = customer("u-1", 0);
        user.setPasswordHash("hash");
        user.setEnabled(false);
        when(userRepository.findByEmail("user@eco-organic-store.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrongpass", "hash")).thenReturn(false);
        when(encoder.matches("secret123", "hash")).thenReturn(true);

        UnauthorizedException wrongPassword = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new LoginRequest("user@eco-organic-store.com", "wrongpass"), new MockHttpServletResponse()));
        assertEquals("Invalid email or password", wrongPassword.getMessage());

        UnauthorizedException disabled = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new LoginRequest("user@eco-organic-store.com", "secret123"), new MockHttpServletResponse()));
        assertEquals("This account is disabled. Contact the store.", disabled.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void googleLoginRejectsADisabledAccount() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), mock(JwtService.class), false, "Lax", AccountMail.none(), mock(PasswordResets.class));
        User user = customer("u-1", 0);
        user.setEnabled(false);
        when(userRepository.findByEmail("user@eco-organic-store.com")).thenReturn(Optional.of(user));

        UnauthorizedException disabled = assertThrows(
                UnauthorizedException.class,
                () -> authService.googleLogin("user@eco-organic-store.com", "User", "sub-1", null, new MockHttpServletResponse()));
        assertEquals("This account is disabled. Contact the store.", disabled.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshRejectsADisabledAccount() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = new JwtService(SECRET);
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax", AccountMail.none(), mock(PasswordResets.class));
        User user = customer("u-1", 2);
        user.setEnabled(false);
        String refresh = jwtService.createRefreshToken("u-1", "user@eco-organic-store.com", List.of("CUSTOMER"), 3600, 2);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthService.REFRESH_COOKIE, refresh));
        UnauthorizedException disabled = assertThrows(
                UnauthorizedException.class,
                () -> authService.refresh(request, new MockHttpServletResponse()));
        assertEquals("This account is disabled. Contact the store.", disabled.getMessage());
    }

    @Test
    void requestResetDelegatesAndKeepsTheReplyGeneric() {
        PasswordResets resets = mock(PasswordResets.class);
        when(resets.request(any())).thenReturn(new MessageResponse(
                "If an account exists, password reset instructions will be sent."));
        AuthService authService = new AuthService(
                mock(UserRepository.class), mock(PasswordEncoder.class), mock(JwtService.class), false, "Lax", AccountMail.none(), resets);

        var requestMessage = authService.requestPasswordReset(new RequestResetRequest("existing@eco-organic-store.com"));

        assertEquals("If an account exists, password reset instructions will be sent.", requestMessage.message());
        verify(resets).request(any());
    }

    private static User customer(String id, int refreshTokenVersion) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@eco-organic-store.com");
        user.setName("User");
        user.setRoles(List.of("CUSTOMER"));
        user.setRefreshTokenVersion(refreshTokenVersion);
        return user;
    }
}
