package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.service.mail.StoreMessages;
import com.ecoorganicstore.identity.web.NotificationDtos.NotificationPreferencesResponse;
import org.springframework.stereotype.Service;

@Service
public class NotificationPreferenceService {
    private final UserRepository users;
    private final StoreMessages messages;

    public NotificationPreferenceService(UserRepository users, StoreMessages messages) {
        this.users = users;
        this.messages = messages;
    }

    public NotificationPreferencesResponse get(String userId) {
        return toResponse(require(userId));
    }

    public NotificationPreferencesResponse update(String userId, boolean orderUpdates) {
        User user = require(userId);
        user.setOrderEmails(orderUpdates);
        return toResponse(users.save(user));
    }

    private User require(String userId) {
        return users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private NotificationPreferencesResponse toResponse(User user) {
        return new NotificationPreferencesResponse(user.wantsOrderEmail(), user.getEmail(), messages.fromAddress());
    }
}
