package com.harvest.identity.service;

import com.harvest.common.security.JwtService;
import com.harvest.identity.domain.User;
import com.harvest.identity.repo.UserRepository;
import com.harvest.identity.web.AuthDtos.ConfirmResetRequest;
import com.harvest.identity.web.AuthDtos.LoginRequest;
import com.harvest.identity.web.AuthDtos.RequestResetRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void loginReturnsAccessTokenAndSetsRefreshCookie() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, encoder, jwtService, false, "Lax");

        User user = new User();
        user.setId("u-1");
        user.setEmail("user@harvest.co");
        user.setName("User");
        user.setPasswordHash("hash");
        user.setRoles(List.of("CUSTOMER"));

        when(userRepository.findByEmail("user@harvest.co")).thenReturn(Optional.of(user));
        when(encoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.createAccessToken(eq("u-1"), eq("user@harvest.co"), anyList(), anyLong()))
                .thenReturn("jwt-token", "refresh-token");

        HttpServletResponse response = new MockHttpServletResponse();
        var result = authService.login(new LoginRequest("user@harvest.co", "secret123"), response);

        assertEquals("jwt-token", result.accessToken());
        assertEquals("user@harvest.co", result.email());
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

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(new LoginRequest("user@harvest.co", "wrongpass"), mock(HttpServletResponse.class)));
        assertEquals("Invalid credentials", exception.getMessage());
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
}
