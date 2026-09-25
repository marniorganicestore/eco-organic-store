package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.identity.domain.Address;
import com.ecoorganicstore.identity.domain.AvatarRef;
import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.web.ProfileDtos.AddressRequest;
import com.ecoorganicstore.identity.web.ProfileDtos.UpdateProfileRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileServiceTest {
    @Test
    void updateChangesProfileFieldsWithoutDroppingAddresses() {
        User user = user();
        user.setAddresses(new ArrayList<>(List.of(saved("addr-1", true))));
        ProfileService service = service(user);

        User updated = service.update("u1", new UpdateProfileRequest("Asha Rao", "https://cdn.example.test/a.png", "9876543210"));

        assertEquals("Asha Rao", updated.getName());
        assertEquals("https://cdn.example.test/a.png", updated.getAvatar());
        assertEquals("9876543210", updated.getPhone());
        assertEquals(1, updated.getAddresses().size());
        assertEquals("addr-1", updated.getAddresses().get(0).getId());
    }

    @Test
    void updateRejectsBlankNameAndUnsafeAvatar() {
        ProfileService service = service(user());

        assertEquals(
                "Name must be 2–80 characters.",
                assertThrows(IllegalArgumentException.class, () -> service.update("u1", new UpdateProfileRequest("  ", null, null)))
                        .getMessage());
        assertEquals(
                "Avatar must be an http(s) link.",
                assertThrows(IllegalArgumentException.class, () -> service.update("u1", new UpdateProfileRequest(null, "javascript:alert(1)", null)))
                        .getMessage());
    }

    @Test
    void blankAvatarAndPhoneClearThoseFields() {
        User user = user();
        user.setAvatar("https://cdn.example.test/a.png");
        user.setPhone("9876543210");
        ProfileService service = service(user);

        User updated = service.update("u1", new UpdateProfileRequest(null, "  ", "  "));

        assertNull(updated.getAvatar());
        assertNull(updated.getPhone());
        assertEquals("Asha", updated.getName());
        assertTrue(updated.hasChosenAvatar());
    }

    @Test
    void replacePhotoStoresANewImageAndDropsThePreviousOne() {
        User user = user();
        user.setAvatar(AvatarRef.path("11111111-1111-1111-1111-111111111111"));
        AvatarService avatars = mock(AvatarService.class);
        when(avatars.store(any())).thenReturn(new AvatarService.Stored(
                "22222222-2222-2222-2222-222222222222",
                AvatarRef.path("22222222-2222-2222-2222-222222222222"),
                "image/jpeg",
                12));
        ProfileService service = service(user, avatars);

        User updated = service.replacePhoto("u1", new byte[] {1, 2, 3});

        assertEquals(AvatarRef.path("22222222-2222-2222-2222-222222222222"), updated.getAvatar());
        assertTrue(updated.hasChosenAvatar());
        verify(avatars).deleteQuietly(AvatarRef.path("11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void removePhotoClearsTheLinkAndTheStoredFile() {
        User user = user();
        user.setAvatar(AvatarRef.path("11111111-1111-1111-1111-111111111111"));
        AvatarService avatars = mock(AvatarService.class);
        ProfileService service = service(user, avatars);

        User updated = service.removePhoto("u1");

        assertNull(updated.getAvatar());
        assertTrue(updated.hasChosenAvatar());
        verify(avatars).deleteQuietly(AvatarRef.path("11111111-1111-1111-1111-111111111111"));
        verify(avatars, never()).store(any());
    }

    @Test
    void firstAddressIsDefaultAndALaterDefaultReplacesIt() {
        ProfileService service = service(user());

        User withHome = service.addAddress("u1", address("Home", false));
        assertTrue(withHome.getAddresses().get(0).isDefaultAddress());

        User withWork = service.addAddress("u1", address("Work", true));
        Address home = find(withWork, "Home");
        Address work = find(withWork, "Work");
        assertFalse(home.isDefaultAddress());
        assertTrue(work.isDefaultAddress());
    }

    @Test
    void deletingTheDefaultPromotesTheNextAddress() {
        ProfileService service = service(user());
        User withHome = service.addAddress("u1", address("Home", true));
        String homeId = find(withHome, "Home").getId();
        service.addAddress("u1", address("Work", false));

        User remaining = service.deleteAddress("u1", homeId);

        assertEquals(1, remaining.getAddresses().size());
        assertEquals("Work", remaining.getAddresses().get(0).getLabel());
        assertTrue(remaining.getAddresses().get(0).isDefaultAddress());
    }

    @Test
    void rejectsANinthAddressAndAnUnknownState() {
        User user = user();
        ArrayList<Address> existing = new ArrayList<>();
        for (int i = 0; i < ProfileService.MAX_ADDRESSES; i++) {
            existing.add(saved("addr-" + i, i == 0));
        }
        user.setAddresses(existing);
        ProfileService service = service(user);

        assertEquals(
                "You can save up to 8 delivery addresses.",
                assertThrows(IllegalArgumentException.class, () -> service.addAddress("u1", address("Extra", false))).getMessage());

        user.setAddresses(new ArrayList<>());
        AddressRequest invalid = new AddressRequest(
                "Home", "Asha Rao", "12 Orchard Lane", "", "Pune", "Narnia", "411001", "", false);
        assertEquals(
                "Choose a state or union territory.",
                assertThrows(IllegalArgumentException.class, () -> service.addAddress("u1", invalid)).getMessage());
    }

    @Test
    void repairsMultipleDefaultsOnRead() {
        User user = user();
        user.setAddresses(List.of(saved("a", true), saved("b", true)));
        ProfileService service = service(user);

        User loaded = service.get("u1");

        assertTrue(loaded.getAddresses().get(0).isDefaultAddress());
        assertFalse(loaded.getAddresses().get(1).isDefaultAddress());
    }

    private static ProfileService service(User user) {
        return service(user, mock(AvatarService.class));
    }

    private static ProfileService service(User user, AvatarService avatars) {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById("u1")).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new ProfileService(repository, avatars);
    }

    private static User user() {
        User user = new User();
        user.setId("u1");
        user.setEmail("asha@eco-organic-store.com");
        user.setName("Asha");
        user.setRoles(List.of("CUSTOMER"));
        return user;
    }

    private static AddressRequest address(String label, boolean defaultAddress) {
        return new AddressRequest(label, "Asha Rao", "12 Orchard Lane", "", "Pune", "Maharashtra", "411001", "9876543210", defaultAddress);
    }

    private static Address saved(String id, boolean defaultAddress) {
        Address address = new Address();
        address.setId(id);
        address.setLabel(id);
        address.setDefaultAddress(defaultAddress);
        return address;
    }

    private static Address find(User user, String label) {
        return user.getAddresses().stream()
                .filter(address -> label.equals(address.getLabel()))
                .findFirst()
                .orElseThrow();
    }
}
