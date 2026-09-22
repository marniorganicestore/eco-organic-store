package com.harvest.identity.service;

import com.harvest.common.web.UnauthorizedException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleIdTokenVerifierService {
    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> GOOGLE_ISSUERS = List.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public GoogleIdTokenVerifierService(@Value("${app.google.client-id:}") String clientId) throws MalformedURLException {
        this.clientId = clientId;
        JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(GOOGLE_JWKS_URL));
        JWSKeySelector<SecurityContext> selector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(selector);
        this.jwtProcessor = processor;
    }

    public GooglePrincipal verify(String idToken) {
        if (clientId == null || clientId.isBlank()) {
            throw new UnauthorizedException("Google login is not configured");
        }
        try {
            JWTClaimsSet claims = jwtProcessor.process(idToken, null);
            if (!GOOGLE_ISSUERS.contains(claims.getIssuer())) {
                throw new UnauthorizedException("Invalid token issuer");
            }
            if (claims.getAudience() == null || !claims.getAudience().contains(clientId)) {
                throw new UnauthorizedException("Token audience mismatch");
            }
            if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw new UnauthorizedException("Token is expired");
            }
            String email = claims.getStringClaim("email");
            String name = claims.getStringClaim("name");
            String picture = claims.getStringClaim("picture");
            String sub = claims.getSubject();
            Boolean emailVerified = claims.getBooleanClaim("email_verified");
            if (email == null || sub == null || !Boolean.TRUE.equals(emailVerified)) {
                throw new UnauthorizedException("Google account is not verified");
            }
            return new GooglePrincipal(sub, email, name == null ? email : name, picture);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid Google ID token");
        }
    }

    public record GooglePrincipal(String subject, String email, String name, String picture) {}
}
