package com.ecoorganicstore.identity.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;
    @Indexed
    private String userId;
    @Indexed(unique = true)
    private String tokenHash;
    /** TTL of 0 seconds: MongoDB deletes the token when this instant is reached. */
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
