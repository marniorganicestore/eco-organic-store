package com.harvest.identity.web;

import com.harvest.common.web.UnauthorizedException;
import com.harvest.identity.service.AuthService;
import com.harvest.identity.service.GoogleIdTokenVerifierService;
import com.harvest.identity.web.AuthDtos.AuthResponse;
import com.harvest.identity.web.AuthDtos.GoogleRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    @Test
    void googleUsesVerifiedPrincipalFromServerSideVerifier() {
        AuthService authService = mock(AuthService.class);
        GoogleIdTokenVerifierService verifier = mock(GoogleIdTokenVerifierService.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthController controller = new AuthController(authService, verifier);
        GoogleIdTokenVerifierService.GooglePrincipal principal =
                new GoogleIdTokenVerifierService.GooglePrincipal("sub-1", "user@harvest.co", "User");
        when(verifier.verify("valid-token")).thenReturn(principal);
        when(authService.googleLogin(eq("user@harvest.co"), eq("User"), eq("sub-1"), any(HttpServletResponse.class)))
                .thenReturn(new AuthResponse("jwt", "u1", "user@harvest.co", "User", List.of("CUSTOMER")));

        AuthResponse result = controller.google(new GoogleRequest("valid-token"), response);

        assertEquals("user@harvest.co", result.email());
    }

    @Test
    void googleRejectsForgedToken() {
        AuthService authService = mock(AuthService.class);
        GoogleIdTokenVerifierService verifier = mock(GoogleIdTokenVerifierService.class);
        AuthController controller = new AuthController(authService, verifier);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(verifier.verify("forged")).thenThrow(new UnauthorizedException("Invalid Google ID token"));

        assertThrows(UnauthorizedException.class, () -> controller.google(new GoogleRequest("forged"), response));
    }
}
