package com.ecoorganicstore.identity.service.mail;

public interface AccountMail {
    void welcome(String name, String email);

    void passwordReset(String name, String email, String resetUrl);

    void passwordChanged(String name, String email);

    void googleSignIn(String name, String email);

    static AccountMail none() {
        return new AccountMail() {
            @Override public void welcome(String name, String email) {}
            @Override public void passwordReset(String name, String email, String resetUrl) {}
            @Override public void passwordChanged(String name, String email) {}
            @Override public void googleSignIn(String name, String email) {}
        };
    }
}
