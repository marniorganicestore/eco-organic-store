package com.ecoorganicstore.identity.web;

import com.ecoorganicstore.common.media.ImageTypes;
import com.ecoorganicstore.identity.domain.AvatarAsset;
import com.ecoorganicstore.identity.service.AvatarService;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/avatars")
public class AvatarController {
    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> avatar(@PathVariable String id) {
        AvatarAsset asset = avatarService.get(id);
        String contentType = asset.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : asset.getContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline; filename=\"profile" + ImageTypes.extension(contentType) + "\"")
                .body(asset.getData());
    }
}
