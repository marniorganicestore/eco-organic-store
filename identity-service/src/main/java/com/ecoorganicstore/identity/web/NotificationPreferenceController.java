package com.ecoorganicstore.identity.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.identity.service.NotificationPreferenceService;
import com.ecoorganicstore.identity.web.NotificationDtos.NotificationPreferencesResponse;
import com.ecoorganicstore.identity.web.NotificationDtos.UpdateNotificationPreferencesRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/notifications")
public class NotificationPreferenceController {
    private final NotificationPreferenceService preferences;

    public NotificationPreferenceController(NotificationPreferenceService preferences) {
        this.preferences = preferences;
    }

    @GetMapping
    public NotificationPreferencesResponse get(HttpServletRequest request) {
        return preferences.get(AuthGuards.requireUser(request).userId());
    }

    @PutMapping
    public NotificationPreferencesResponse update(
            HttpServletRequest request, @Valid @RequestBody UpdateNotificationPreferencesRequest body) {
        return preferences.update(AuthGuards.requireUser(request).userId(), body.orderUpdates());
    }
}
