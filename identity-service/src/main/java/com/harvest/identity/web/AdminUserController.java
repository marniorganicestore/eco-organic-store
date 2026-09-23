package com.harvest.identity.web;

import com.harvest.common.security.AuthGuards;
import com.harvest.common.security.UserContext;
import com.harvest.identity.service.AccountAccessService;
import com.harvest.identity.web.AdminUserDtos.AdminUserResponse;
import com.harvest.identity.web.AdminUserDtos.UpdateUserAccessRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
    public List<AdminUserResponse> list(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
        return accountAccessService.list();
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
