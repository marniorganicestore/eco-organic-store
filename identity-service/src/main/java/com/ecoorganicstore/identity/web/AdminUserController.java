package com.ecoorganicstore.identity.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.common.security.UserContext;
import com.ecoorganicstore.common.web.PageResponse;
import com.ecoorganicstore.identity.service.AccountAccessService;
import com.ecoorganicstore.identity.web.AdminUserDtos.AdminUserResponse;
import com.ecoorganicstore.identity.web.AdminUserDtos.UpdateUserAccessRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUserController {
    private final AccountAccessService accountAccessService;

    public AdminUserController(AccountAccessService accountAccessService) {
        this.accountAccessService = accountAccessService;
    }

    @GetMapping("/api/admin/users")
    public PageResponse<AdminUserResponse> list(HttpServletRequest request,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        AuthGuards.requireAdmin(request);
        return accountAccessService.list(page, size);
    }

    @PatchMapping("/api/admin/users/{userId}")
    public AdminUserResponse update(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserAccessRequest body,
            HttpServletRequest request) {
        UserContext actor = AuthGuards.requireAdmin(request);
        return accountAccessService.update(actor.userId(), userId, body);
    }
}
