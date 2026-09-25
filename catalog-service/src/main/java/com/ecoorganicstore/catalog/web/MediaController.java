package com.ecoorganicstore.catalog.web;

import com.ecoorganicstore.catalog.domain.MediaAsset;
import com.ecoorganicstore.catalog.service.MediaService;
import com.ecoorganicstore.common.media.ImageTypes;
import com.ecoorganicstore.catalog.service.MediaService.StoredMedia;
import com.ecoorganicstore.common.security.AuthGuards;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
public class MediaController {
    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/api/admin/catalog/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaUploadResponse upload(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        AuthGuards.requireAdmin(request);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a photo to upload.");
        }
        try {
            StoredMedia stored = mediaService.store(file.getBytes());
            return new MediaUploadResponse(stored.id(), stored.url(), stored.contentType(), stored.size());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Choose a photo to upload.");
        }
    }

    @GetMapping("/api/media/{id}")
    public ResponseEntity<byte[]> media(@PathVariable String id) {
        MediaAsset asset = mediaService.get(id);
        String contentType = asset.getContentType() == null ? "application/octet-stream" : asset.getContentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline; filename=\"photo" + ImageTypes.extension(contentType) + "\"")
                .body(asset.getData());
    }

    public record MediaUploadResponse(String id, String url, String contentType, long size) {}
}
