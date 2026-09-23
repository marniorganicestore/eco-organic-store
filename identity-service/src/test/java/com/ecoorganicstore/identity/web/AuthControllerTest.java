package com.ecoorganicstore.identity.web;

import com.ecoorganicstore.common.web.UnauthorizedException;
import com.ecoorganicstore.identity.service.AuthService;
import com.ecoorganicstore.identity.service.GoogleIdTokenVerifierService;
import com.ecoorganicstore.identity.web.AuthDtos.AuthResponse;
import com.ecoorganicstore.identity.web.AuthDtos.GoogleRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    @Test
    void googleUsesVerifiedPrincipalFromServerSideVerifier() {
        AuthService authService = mock(AuthService.class);
        GoogleIdTokenVerifierService verifier = mock(GoogleIdTokenVerifierService.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthController controller = new AuthController(authService, verifier);
        GoogleIdTokenVerifierService.GooglePrincipal principal =
                new GoogleIdTokenVerifierService.GooglePrincipal("sub-1", "user@eco-organic-store.com", "User", "https://img.test/a.png");
        when(verifier.verify("valid-token")).thenReturn(principal);
        when(authService.googleLogin(
                        eq("user@eco-organic-store.com"),
                        eq("User"),
                        eq("sub-1"),
                        eq("https://img.test/a.png"),
                        any(HttpServletResponse.class)))
                .thenReturn(new AuthResponse("jwt", "u1", "user@eco-organic-store.com", "User", List.of("CUSTOMER"), "https://img.test/a.png"));

        AuthResponse result = controller.google(new GoogleRequest("valid-token"), response);

        assertEquals("user@eco-organic-store.com", result.email());
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

    @Test
    void logoutReturnsNoContentAndDelegatesToService() {
        AuthService authService = mock(AuthService.class);
        GoogleIdTokenVerifierService verifier = mock(GoogleIdTokenVerifierService.class);
        AuthController controller = new AuthController(authService, verifier);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        var result = controller.logout(request, response);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(authService).logout(request, response);
    }
}
