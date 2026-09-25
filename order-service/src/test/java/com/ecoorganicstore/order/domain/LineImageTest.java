package com.ecoorganicstore.order.domain;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LineImageTest {
    @Test
    void snapshotKeepsTheFirstPublicPhoto() {
        assertEquals("https://images.example/honey.jpg", LineImage.snapshot(List.of(" https://images.example/honey.jpg ", "https://images.example/two.jpg")));
        assertEquals("/api/media/honey", LineImage.snapshot(List.of("/api/media/honey")));
    }

    @Test
    void snapshotDropsAnythingThatIsNotAPublicImageAddress() {
        assertNull(LineImage.snapshot(List.of("javascript:alert(1)")));
        assertNull(LineImage.snapshot(List.of("/api/media/../secret")));
        assertNull(LineImage.snapshot(null));
        assertNull(LineImage.snapshot(List.of()));
    }
}
