package com.ecoorganicstore.catalog.service;

import com.ecoorganicstore.catalog.domain.MediaAsset;
import com.ecoorganicstore.catalog.repo.MediaRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MediaService {
    private final MediaRepository mediaRepository;

    public MediaService(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    public StoredMedia store(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Choose a photo to upload.");
        }
        if (bytes.length > ImageTypes.MAX_BYTES) {
            throw new IllegalArgumentException("Use a photo under 1.5 MB.");
        }
        String contentType = ImageTypes.contentType(bytes);
        MediaAsset asset = new MediaAsset();
        asset.setId(UUID.randomUUID().toString());
        asset.setContentType(contentType);
        asset.setSize(bytes.length);
        asset.setData(bytes);
        mediaRepository.save(asset);
        return new StoredMedia(asset.getId(), "/api/media/" + asset.getId(), contentType, bytes.length);
    }

    public MediaAsset get(String id) {
        return mediaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Photo not found."));
    }

    public record StoredMedia(String id, String url, String contentType, long size) {}
}
