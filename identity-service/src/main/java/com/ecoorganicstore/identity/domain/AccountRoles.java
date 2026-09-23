package com.ecoorganicstore.identity.domain;

import com.ecoorganicstore.common.security.Roles;
import java.util.List;
import java.util.Locale;

/**
 * Every account can shop. Admin is an extra grant, never a self-serve registration choice.
 */
public final class AccountRoles {
    private AccountRoles() {}

    public static List<String> customer() {
        return List.of(Roles.CUSTOMER);
    }

    public static List<String> forToken(List<String> stored) {
        return isAdmin(stored) ? List.of(Roles.CUSTOMER, Roles.ADMIN) : customer();
    }

    public static List<String> assign(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new IllegalArgumentException("Choose Customer or Admin.");
        }
        boolean admin = false;
        boolean recognized = false;
        for (String role : requested) {
            String normalized = normalize(role);
            if (normalized == null) {
                continue;
            }
            if (Roles.CUSTOMER.equals(normalized)) {
                recognized = true;
            } else if (Roles.ADMIN.equals(normalized)) {
                recognized = true;
                admin = true;
            } else {
                throw new IllegalArgumentException("Unknown role.");
            }
        }
        if (!recognized) {
            throw new IllegalArgumentException("Choose Customer or Admin.");
        }
        return admin ? List.of(Roles.CUSTOMER, Roles.ADMIN) : customer();
    }

    public static boolean isAdmin(List<String> roles) {
        if (roles == null) {
            return false;
        }
        for (String role : roles) {
            if (Roles.ADMIN.equals(normalize(role))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }
}
