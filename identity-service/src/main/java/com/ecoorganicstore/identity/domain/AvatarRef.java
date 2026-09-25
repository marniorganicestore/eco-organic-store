package com.ecoorganicstore.identity.domain;

import java.util.UUID;

/** Public path for a photo stored in the identity database. */
public final class AvatarRef {
    public static final String PREFIX = "/api/avatars/";

    private AvatarRef() {}

    public static String path(String id) {
        return PREFIX + id;
    }

    public static boolean isOwned(String avatar) {
        return idOf(avatar) != null;
    }

    public static String idOf(String avatar) {
        if (avatar == null || !avatar.startsWith(PREFIX)) {
            return null;
        }
        String id = avatar.substring(PREFIX.length());
        if (id.isBlank() || id.indexOf('/') >= 0 || id.indexOf('\\') >= 0 || id.indexOf('.') >= 0 || id.indexOf('?') >= 0) {
            return null;
        }
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return id;
    }
}
