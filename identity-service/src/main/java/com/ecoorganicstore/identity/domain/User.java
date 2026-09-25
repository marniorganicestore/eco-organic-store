package com.ecoorganicstore.identity.domain;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
@CompoundIndex(name = "email_id_idx", def = "{'email':1,'_id':1}")
public class User {
    @Id
    private String id;
    @Indexed(unique = true)
    private String email;
    private String name;
    private String avatar;
    /** True once the customer uploads, links, or removes a photo. Google must not overwrite that choice. */
    private Boolean avatarChosen;
    private String phone;
    private String passwordHash;
    private String googleSub;
    private List<String> roles = new ArrayList<>(List.of("CUSTOMER"));
    /** Null means enabled so documents created before this flag still sign in. */
    private Boolean enabled;
    private List<Address> addresses = new ArrayList<>();
    private int refreshTokenVersion = 0;
    /** Null means the customer still wants order mail. */
    private Boolean orderEmails;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public boolean hasChosenAvatar() { return Boolean.TRUE.equals(avatarChosen); }
    public void setAvatarChosen(boolean avatarChosen) { this.avatarChosen = avatarChosen; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getGoogleSub() { return googleSub; }
    public void setGoogleSub(String googleSub) { this.googleSub = googleSub; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public boolean isEnabled() { return !Boolean.FALSE.equals(enabled); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<Address> getAddresses() {
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses == null ? new ArrayList<>() : addresses;
    }
    public int getRefreshTokenVersion() { return refreshTokenVersion; }
    public void setRefreshTokenVersion(int refreshTokenVersion) { this.refreshTokenVersion = refreshTokenVersion; }
    public boolean wantsOrderEmail() { return !Boolean.FALSE.equals(orderEmails); }
    public void setOrderEmails(boolean orderEmails) { this.orderEmails = orderEmails; }
}