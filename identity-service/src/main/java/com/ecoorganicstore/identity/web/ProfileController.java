package com.ecoorganicstore.identity.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.identity.service.AuthService;
import com.ecoorganicstore.identity.service.ProfileService;
import com.ecoorganicstore.identity.web.AuthDtos.AuthResponse;
import com.ecoorganicstore.identity.web.AuthDtos.ChangePasswordRequest;
import com.ecoorganicstore.identity.web.ProfileDtos.AddressRequest;
import com.ecoorganicstore.identity.web.ProfileDtos.ProfileResponse;
import com.ecoorganicstore.identity.web.ProfileDtos.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
public class ProfileController {
    private final ProfileService profileService;
    private final AuthService authService;

    public ProfileController(ProfileService profileService, AuthService authService) {
        this.profileService = profileService;
        this.authService = authService;
    }

    @GetMapping
    public ProfileResponse me(HttpServletRequest request) {
        return ProfileMapper.toProfile(profileService.get(AuthGuards.requireUser(request).userId()));
    }

    @PatchMapping
    public ProfileResponse update(HttpServletRequest request, @Valid @RequestBody UpdateProfileRequest body) {
        return ProfileMapper.toProfile(profileService.update(AuthGuards.requireUser(request).userId(), body));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a photo to upload.");
        }
        try {
            return ProfileMapper.toProfile(profileService.replacePhoto(AuthGuards.requireUser(request).userId(), file.getBytes()));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Choose a photo to upload.");
        }
    }

    @DeleteMapping("/avatar")
    public ProfileResponse removeAvatar(HttpServletRequest request) {
        return ProfileMapper.toProfile(profileService.removePhoto(AuthGuards.requireUser(request).userId()));
    }

    @PostMapping("/password")
    public AuthResponse changePassword(
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid @RequestBody ChangePasswordRequest body) {
        return authService.changePassword(AuthGuards.requireUser(request).userId(), body, response);
    }

    @PostMapping("/addresses")
    public ProfileResponse addAddress(HttpServletRequest request, @Valid @RequestBody AddressRequest body) {
        return ProfileMapper.toProfile(profileService.addAddress(AuthGuards.requireUser(request).userId(), body));
    }

    @PatchMapping("/addresses/{addressId}")
    public ProfileResponse updateAddress(
            HttpServletRequest request,
            @PathVariable String addressId,
            @Valid @RequestBody AddressRequest body) {
        return ProfileMapper.toProfile(profileService.updateAddress(AuthGuards.requireUser(request).userId(), addressId, body));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ProfileResponse deleteAddress(HttpServletRequest request, @PathVariable String addressId) {
        return ProfileMapper.toProfile(profileService.deleteAddress(AuthGuards.requireUser(request).userId(), addressId));
    }

    @PostMapping("/addresses/{addressId}/default")
    public ProfileResponse makeDefault(HttpServletRequest request, @PathVariable String addressId) {
        return ProfileMapper.toProfile(profileService.makeDefault(AuthGuards.requireUser(request).userId(), addressId));
    }
}
