package com.ecoorganicstore.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = "com.ecoorganicstore",
        excludeName = "org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration")
public class IdentityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}