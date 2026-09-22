package com.harvest.identity.domain;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String email;
    private String name;
    private String avatar;
    private String passwordHash;
    private String googleSub;
    private List<String> roles = new ArrayList<>(List.of("CUSTOMER"));
    private List<String> addresses = new ArrayList<>();
    private int refreshTokenVersion = 0;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<String> getAddresses() { return addresses; }
    public void setAddresses(List<String> addresses) { this.addresses = addresses; }
    public int getRefreshTokenVersion() { return refreshTokenVersion; }
    public void setRefreshTokenVersion(int refreshTokenVersion) { this.refreshTokenVersion = refreshTokenVersion; }
}