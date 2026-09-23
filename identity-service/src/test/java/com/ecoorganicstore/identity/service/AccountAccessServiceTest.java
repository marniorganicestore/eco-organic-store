package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.web.AdminUserDtos.UpdateUserAccessRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountAccessServiceTest {
    @Test
    void grantAdminKeepsCustomerAndRevokesThePreviousSession() {
        UserRepository userRepository = mock(UserRepository.class);
        AccountAccessService service = new AccountAccessService(userRepository);
        User customer = user("u-2", "ada@eco-organic-store.com", List.of("CUSTOMER"), true, 1);
        User actor = user("u-1", "admin@eco-organic-store.com", List.of("CUSTOMER", "ADMIN"), true, 0);
        when(userRepository.findById("u-2")).thenReturn(Optional.of(customer));
        when(userRepository.findAll()).thenReturn(List.of(actor, customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update("u-1", "u-2", new UpdateUserAccessRequest(List.of("admin"), null));

        assertEquals(List.of("CUSTOMER", "ADMIN"), updated.roles());
        assertEquals(2, customer.getRefreshTokenVersion());
        assertTrue(customer.isEnabled());
    }

    @Test
    void rejectsUnknownRolesSelfLockoutAndTheLastAdmin() {
        UserRepository userRepository = mock(UserRepository.class);
        AccountAccessService service = new AccountAccessService(userRepository);
        User admin = user("u-1", "admin@eco-organic-store.com", List.of("ADMIN"), true, 0);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(admin));
        when(userRepository.findAll()).thenReturn(List.of(admin));

        IllegalArgumentException unknown = assertThrows(
                IllegalArgumentException.class,
                () -> service.update("u-1", "u-1", new UpdateUserAccessRequest(List.of("STAFF"), null)));
        assertEquals("Unknown role.", unknown.getMessage());

        IllegalArgumentException selfDemotion = assertThrows(
                IllegalArgumentException.class,
                () -> service.update("u-1", "u-1", new UpdateUserAccessRequest(List.of("CUSTOMER"), null)));
        assertEquals("You cannot remove your own admin access.", selfDemotion.getMessage());

        IllegalArgumentException selfDisable = assertThrows(
                IllegalArgumentException.class,
                () -> service.update("u-1", "u-1", new UpdateUserAccessRequest(null, false)));
        assertEquals("You cannot disable your own account.", selfDisable.getMessage());
        verify(userRepository, never()).save(any());
        assertTrue(admin.isEnabled());
    }

    @Test
    void keepsTheLastActiveAdmin() {
        UserRepository userRepository = mock(UserRepository.class);
        AccountAccessService service = new AccountAccessService(userRepository);
        User onlyAdmin = user("u-1", "admin@eco-organic-store.com", List.of("CUSTOMER", "ADMIN"), true, 4);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(onlyAdmin));
        when(userRepository.findAll()).thenReturn(List.of(onlyAdmin));

        IllegalArgumentException lastAdmin = assertThrows(
                IllegalArgumentException.class,
                () -> service.update("u-stale", "u-1", new UpdateUserAccessRequest(null, false)));
        assertEquals("Keep at least one active admin account.", lastAdmin.getMessage());
        assertTrue(onlyAdmin.isEnabled());
        verify(userRepository, never()).save(any());
    }

    private static User user(String id, String email, List<String> roles, boolean enabled, int version) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(email);
        user.setRoles(roles);
        user.setEnabled(enabled);
        user.setRefreshTokenVersion(version);
        return user;
    }
}
