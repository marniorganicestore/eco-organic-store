package com.ecoorganicstore.common.media;

/**
 * Accepts only raster photos. The bytes are sniffed so a renamed file cannot be stored and served.
 */
public final class ImageTypes {
    public static final int MAX_BYTES = 1_572_864;

    private ImageTypes() {}

    public static String contentType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw new IllegalArgumentException("Use a JPEG, PNG, or WebP photo.");
        }
        if (isJpeg(bytes)) return "image/jpeg";
        if (isPng(bytes)) return "image/png";
        if (isWebp(bytes)) return "image/webp";
        throw new IllegalArgumentException("Use a JPEG, PNG, or WebP photo.");
    }

    public static String extension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private static boolean isJpeg(byte[] bytes) {
        return (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] bytes) {
        return bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
    }

    private static boolean isWebp(byte[] bytes) {
        return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
