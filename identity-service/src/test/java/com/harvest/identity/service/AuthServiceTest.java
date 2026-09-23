package com.harvest.identity.service;

import com.harvest.common.security.JwtService;
import com.harvest.common.web.UnauthorizedException;
import com.harvest.identity.domain.User;
import com.harvest.identity.repo.UserRepository;
import com.harvest.identity.web.AuthDtos.ChangePasswordRequest;
import com.harvest.identity.web.AuthDtos.ConfirmResetRequest;
import com.harvest.identity.web.AuthDtos.LoginRequest;
import com.harvest.identity.web.AuthDtos.RequestResetRequest;
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
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax");

        User user = customer("u-1", 0);
        user.setPasswordHash("hash");

        when(userRepository.findByEmail("user@harvest.co")).thenReturn(Optional.of(user));
        when(encoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.createAccessToken(eq("u-1"), eq("user@harvest.co"), anyList(), anyLong()))
                .thenReturn("jwt-token");
        when(jwtService.createRefreshToken(eq("u-1"), eq("user@harvest.co"), anyList(), anyLong(), eq(0)))
                .thenReturn("refresh-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        var result = authService.login(new LoginRequest("user@harvest.co", "secret123"), response);

        assertEquals("jwt-token", result.accessToken());
        assertEquals("user@harvest.co", result.email());
        assertTrue(response.getHeader("Set-Cookie").contains("refreshToken=refresh-token"));
        assertTrue(response.getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    void loginRejectsWrongPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax");

        User user = new User();
        user.setEmail("user@harvest.co");
        user.setPasswordHash("hash");
        when(userRepository.findByEmail("user@harvest.co")).thenReturn(Optional.of(user));
        when(encoder.matches("wrongpass", "hash")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new LoginRequest("user@harvest.co", "wrongpass"), new MockHttpServletResponse()));
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void loginRejectsUnknownEmailWithSameMessage() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax");

        when(userRepository.findByEmail("missing@harvest.co")).thenReturn(Optional.empty());
        when(encoder.encode("harvest-not-a-user")).thenReturn("dummy-hash");
        when(encoder.matches("secret123", "dummy-hash")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(new LoginRequest("missing@harvest.co", "secret123"), new MockHttpServletResponse()));
        assertEquals("Invalid email or password", exception.getMessage());
        verify(encoder).matches("secret123", "dummy-hash");
    }

    @Test
    void googleLoginStoresAvatarAndIssuesTokens() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax");

        when(userRepository.findByEmail("user@harvest.co")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId("u-1");
            return saved;
        });
        when(jwtService.createAccessToken(eq("u-1"), eq("user@harvest.co"), anyList(), anyLong())).thenReturn("jwt-token");
        when(jwtService.createRefreshToken(eq("u-1"), eq("user@harvest.co"), anyList(), anyLong(), eq(0)))
                .thenReturn("refresh-token");

        var result = authService.googleLogin(
                "user@harvest.co", "User", "sub-1", "https://img.test/a.png", new MockHttpServletResponse());

        assertEquals("jwt-token", result.accessToken());
        assertEquals("https://img.test/a.png", result.avatar());
    }

    @Test
    void logoutRevokesRefreshFamilyAndClearsCookie() {
        UserRepository userRepository = mock(UserRepository.class);
        JwtService jwtService = new JwtService(SECRET);
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax");

        User user = customer("u-1", 3);
        String refresh = jwtService.createRefreshToken("u-1", "user@harvest.co", List.of("CUSTOMER"), 3600, 3);
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
                userRepository, mock(PasswordEncoder.class), new JwtService(SECRET), false, "Lax");

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
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax");

        User user = customer("u-1", 5);
        String staleRefresh = jwtService.createRefreshToken("u-1", "user@harvest.co", List.of("CUSTOMER"), 3600, 4);
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
        AuthService authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, false, "Lax");

        String access = jwtService.createAccessToken("u-1", "user@harvest.co", List.of("CUSTOMER"), 600);

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
                mock(UserRepository.class), mock(PasswordEncoder.class), new JwtService(SECRET), false, "Lax");

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
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax");
        User user = customer("u-1", 2);
        user.setPasswordHash("hash");
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.matches("current-password", "hash")).thenReturn(true);
        when(encoder.matches("new-password-1", "hash")).thenReturn(false);
        when(encoder.encode("new-password-1")).thenReturn("new-hash");
        when(jwtService.createAccessToken(eq("u-1"), eq("user@harvest.co"), anyList(), anyLong())).thenReturn("next-access");
        when(jwtService.createRefreshToken(eq("u-1"), eq("user@harvest.co"), anyList(), anyLong(), eq(3))).thenReturn("next-refresh");

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
        AuthService authService = new AuthService(userRepository, encoder, mock(JwtService.class), false, "Lax");
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
    void requestResetAndConfirmResetReturnGenericMessages() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax");

        User existing = new User();
        existing.setId("u-99");
        when(userRepository.findByEmail("existing@harvest.co")).thenReturn(Optional.of(existing));

        var requestMessage = authService.requestPasswordReset(new RequestResetRequest("existing@harvest.co"));
        var confirmMessage = authService.confirmPasswordReset(new ConfirmResetRequest("token-shell", "newPassword123"));

        assertEquals("If an account exists, password reset instructions will be sent.", requestMessage.message());
        assertEquals("Password reset request accepted.", confirmMessage.message());
        verify(userRepository).findByEmail("existing@harvest.co");
    }

    private static User customer(String id, int refreshTokenVersion) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@harvest.co");
        user.setName("User");
        user.setRoles(List.of("CUSTOMER"));
        user.setRefreshTokenVersion(refreshTokenVersion);
        return user;
    }
}
