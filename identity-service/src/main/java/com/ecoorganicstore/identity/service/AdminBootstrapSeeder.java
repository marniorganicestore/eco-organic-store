package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.common.security.Roles;
import com.ecoorganicstore.identity.domain.AccountRoles;
import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminName;
    private final String adminPassword;
    private final String adminPasswordHash;

    public AdminBootstrapSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:admin@eco-organic-store.com}") String adminEmail,
            @Value("${app.admin.name:Store Admin}") String adminName,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.admin.password-hash:}") String adminPasswordHash) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminName = adminName;
        this.adminPassword = adminPassword;
        this.adminPasswordHash = adminPasswordHash;
    }

    @Override
    public void run(String... args) {
        User admin = userRepository.findByEmail(adminEmail.trim().toLowerCase(Locale.ROOT)).orElseGet(User::new);
        admin.setEmail(adminEmail.trim().toLowerCase(Locale.ROOT));
        admin.setName(adminName);
        List<String> roles = admin.getRoles() == null ? new ArrayList<>() : new ArrayList<>(admin.getRoles());
        if (!AccountRoles.isAdmin(roles)) {
            roles.add(Roles.ADMIN);
        }
        admin.setRoles(AccountRoles.forToken(roles));
        admin.setEnabled(true);
        if (adminPasswordHash != null && !adminPasswordHash.isBlank()) {
            admin.setPasswordHash(adminPasswordHash);
        } else if (adminPassword != null && !adminPassword.isBlank()) {
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        }
        userRepository.save(admin);
    }
}
