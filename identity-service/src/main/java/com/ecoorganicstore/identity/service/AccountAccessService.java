package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.common.web.PageResponse;
import com.ecoorganicstore.common.web.PageWindow;
import com.ecoorganicstore.identity.domain.AccountRoles;
import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.web.AdminUserDtos.AdminUserResponse;
import com.ecoorganicstore.identity.web.AdminUserDtos.UpdateUserAccessRequest;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AccountAccessService {
    private final UserRepository userRepository;

    public AccountAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PageResponse<AdminUserResponse> list(int page, int size) {
        int safePage = PageWindow.page(page);
        int safeSize = PageWindow.size(size, PageWindow.ADMIN_SIZE);
        var result = userRepository.findAll(PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.asc("email"), Sort.Order.asc("id"))));
        List<AdminUserResponse> items = result.getContent().stream().map(AccountAccessService::toResponse).toList();
        return PageResponse.of(items, safePage, safeSize, result.getTotalElements());
    }

    public AdminUserResponse update(String actorId, String userId, UpdateUserAccessRequest request) {
        boolean rolesProvided = request.roles() != null && !request.roles().isEmpty();
        if (!rolesProvided && request.enabled() == null) {
            throw new IllegalArgumentException("Choose a role or account status.");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        List<String> nextRoles = rolesProvided ? AccountRoles.assign(request.roles()) : AccountRoles.forToken(user.getRoles());
        boolean nextEnabled = request.enabled() == null ? user.isEnabled() : request.enabled();
        boolean activeAdmin = user.isEnabled() && AccountRoles.isAdmin(user.getRoles());
        boolean staysActiveAdmin = nextEnabled && AccountRoles.isAdmin(nextRoles);

        if (actorId.equals(user.getId())) {
            if (activeAdmin && !AccountRoles.isAdmin(nextRoles)) {
                throw new IllegalArgumentException("You cannot remove your own admin access.");
            }
            if (user.isEnabled() && !nextEnabled) {
                throw new IllegalArgumentException("You cannot disable your own account.");
            }
        }
        if (activeAdmin && !staysActiveAdmin && !anotherActiveAdmin(user.getId())) {
            throw new IllegalArgumentException("Keep at least one active admin account.");
        }

        boolean changed = !nextRoles.equals(AccountRoles.forToken(user.getRoles())) || nextEnabled != user.isEnabled();
        if (changed) {
            user.setRoles(nextRoles);
            user.setEnabled(nextEnabled);
            user.setRefreshTokenVersion(user.getRefreshTokenVersion() + 1);
            userRepository.save(user);
        }
        return toResponse(user);
    }

    private boolean anotherActiveAdmin(String exceptUserId) {
        return userRepository.findAll().stream()
                .anyMatch(other -> other.getId() != null
                        && !other.getId().equals(exceptUserId)
                        && other.isEnabled()
                        && AccountRoles.isAdmin(other.getRoles()));
    }

    private static AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatar(),
                AccountRoles.forToken(user.getRoles()),
                user.isEnabled());
    }
}
