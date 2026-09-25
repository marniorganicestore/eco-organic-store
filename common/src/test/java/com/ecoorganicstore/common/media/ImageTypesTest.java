package com.ecoorganicstore.common.media;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageTypesTest {
    @Test
    void acceptsJpegPngAndWebpFromTheirSignatures() {
        assertEquals("image/jpeg", ImageTypes.contentType(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        assertEquals("image/png", ImageTypes.contentType(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0}));
        byte[] webp = new byte[12];
        webp[0] = 'R';
        webp[1] = 'I';
        webp[2] = 'F';
        webp[3] = 'F';
        webp[8] = 'W';
        webp[9] = 'E';
        webp[10] = 'B';
        webp[11] = 'P';
        assertEquals("image/webp", ImageTypes.contentType(webp));
    }

    @Test
    void rejectsAFileThatIsNotAPhoto() {
        byte[] html = "<html>hi</html>".getBytes();
        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class, () -> ImageTypes.contentType(html));
        assertEquals("Use a JPEG, PNG, or WebP photo.", rejected.getMessage());
    }
}
