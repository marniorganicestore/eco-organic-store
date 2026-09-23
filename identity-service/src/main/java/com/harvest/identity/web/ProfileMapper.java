package com.harvest.identity.web;

import com.harvest.identity.domain.Address;
import com.harvest.identity.domain.User;
import com.harvest.identity.web.AuthDtos.UserResponse;
import com.harvest.identity.web.ProfileDtos.AddressResponse;
import com.harvest.identity.web.ProfileDtos.ProfileResponse;
import java.util.Comparator;
import java.util.List;

public final class ProfileMapper {
    private ProfileMapper() {}

    public static ProfileResponse toProfile(User user) {
        List<String> roles = user.getRoles() == null ? List.of() : List.copyOf(user.getRoles());
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getAvatar(),
                blankToNull(user.getPhone()),
                roles,
                hasText(user.getPasswordHash()),
                hasText(user.getGoogleSub()),
                toAddresses(user.getAddresses()));
    }

    public static UserResponse toUser(User user) {
        ProfileResponse profile = toProfile(user);
        return new UserResponse(
                profile.userId(), profile.email(), profile.name(), profile.avatar(), profile.roles(), profile.addresses());
    }

    private static List<AddressResponse> toAddresses(List<Address> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }
        return addresses.stream()
                .sorted(Comparator.comparing(Address::isDefaultAddress).reversed())
                .map(ProfileMapper::toAddress)
                .toList();
    }

    private static AddressResponse toAddress(Address address) {
        return new AddressResponse(
                address.getId(),
                text(address.getLabel()),
                text(address.getRecipient()),
                text(address.getLine1()),
                text(address.getLine2()),
                text(address.getCity()),
                text(address.getState()),
                text(address.getPostalCode()),
                text(address.getPhone()),
                address.isDefaultAddress());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
