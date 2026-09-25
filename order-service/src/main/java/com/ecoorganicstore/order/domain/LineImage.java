package com.ecoorganicstore.order.domain;

import java.net.URI;
import java.util.List;

/**
 * The first public catalog photo, frozen on the order line at checkout.
 * Later catalog edits must not change what the customer ordered.
 */
public final class LineImage {
    private LineImage() {}

    public static String snapshot(Object images) {
        if (!(images instanceof List<?> list) || list.isEmpty() || !(list.getFirst() instanceof String raw)) {
            return null;
        }
        String image = raw.trim();
        if (image.isEmpty() || image.length() > 500) return null;
        if (image.startsWith("/api/media/") && !image.contains("..") && !image.contains("\\") && !image.contains("?")) {
            return image;
        }
        try {
            URI uri = URI.create(image);
            String scheme = uri.getScheme();
            if (("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) && uri.getHost() != null) {
                return image;
            }
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return null;
    }
}
