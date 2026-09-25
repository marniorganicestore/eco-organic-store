package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.common.media.ImageTypes;
import com.ecoorganicstore.identity.domain.AvatarAsset;
import com.ecoorganicstore.identity.domain.AvatarRef;
import com.ecoorganicstore.identity.repo.AvatarRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AvatarService {
    private final AvatarRepository avatarRepository;

    public AvatarService(AvatarRepository avatarRepository) {
        this.avatarRepository = avatarRepository;
    }

    public Stored store(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Choose a photo to upload.");
        }
        if (bytes.length > ImageTypes.MAX_BYTES) {
            throw new IllegalArgumentException("Use a photo under 1.5 MB.");
        }
        String contentType = ImageTypes.contentType(bytes);
        AvatarAsset asset = new AvatarAsset();
        asset.setId(UUID.randomUUID().toString());
        asset.setContentType(contentType);
        asset.setSize(bytes.length);
        asset.setData(bytes);
        asset.setUpdatedAt(Instant.now());
        avatarRepository.save(asset);
        return new Stored(asset.getId(), AvatarRef.path(asset.getId()), contentType, bytes.length);
    }

    public AvatarAsset get(String id) {
        String owned = AvatarRef.idOf(AvatarRef.path(id));
        if (owned == null) {
            throw new IllegalArgumentException("Photo not found.");
        }
        return avatarRepository.findById(owned).orElseThrow(() -> new IllegalArgumentException("Photo not found."));
    }

    public void deleteQuietly(String avatarUrl) {
        String id = AvatarRef.idOf(avatarUrl);
        if (id != null) {
            avatarRepository.deleteById(id);
        }
    }

    public record Stored(String id, String url, String contentType, long size) {}
}
