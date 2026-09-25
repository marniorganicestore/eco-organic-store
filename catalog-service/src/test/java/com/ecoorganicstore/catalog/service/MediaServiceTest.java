package com.ecoorganicstore.catalog.service;

import com.ecoorganicstore.catalog.domain.MediaAsset;
import com.ecoorganicstore.catalog.repo.MediaRepository;
import com.ecoorganicstore.common.media.ImageTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaServiceTest {
    @Test
    void storeKeepsTheDetectedTypeAndAPublicMediaPath() {
        MediaRepository repository = mock(MediaRepository.class);
        when(repository.save(any(MediaAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MediaService service = new MediaService(repository);

        MediaService.StoredMedia stored = service.store(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5, 6, 7, 8, 9});

        assertEquals("image/jpeg", stored.contentType());
        assertTrue(stored.url().startsWith("/api/media/"));
        assertEquals(12, stored.size());
    }

    @Test
    void storeRejectsAnOversizedPhotoBeforeSaving() {
        MediaRepository repository = mock(MediaRepository.class);
        MediaService service = new MediaService(repository);

        IllegalArgumentException rejected = assertThrows(IllegalArgumentException.class, () -> service.store(new byte[ImageTypes.MAX_BYTES + 1]));

        assertEquals("Use a photo under 1.5 MB.", rejected.getMessage());
        verify(repository, never()).save(any());
    }
}
