package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.identity.domain.Address;
import com.ecoorganicstore.identity.domain.IndianStates;
import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.web.ProfileDtos.AddressRequest;
import com.ecoorganicstore.identity.web.ProfileDtos.UpdateProfileRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    static final int MAX_ADDRESSES = 8;
    private static final Pattern PHONE = Pattern.compile("^(?:\\+91[\\s-]?)?[6-9]\\d{9}$");
    private static final Pattern PIN = Pattern.compile("[1-9][0-9]{5}");

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get(String userId) {
        User user = requireUser(userId);
        if (normalizeDefaults(user)) {
            user = userRepository.save(user);
        }
        return user;
    }

    public User update(String userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        if (request.name() != null) {
            user.setName(requireLength(request.name(), 2, 80, "Name must be 2–80 characters."));
        }
        if (request.avatar() != null) {
            user.setAvatar(normalizeAvatar(request.avatar()));
        }
        if (request.phone() != null) {
            user.setPhone(normalizePhone(request.phone()));
        }
        normalizeDefaults(user);
        return userRepository.save(user);
    }

    public User addAddress(String userId, AddressRequest request) {
        User user = requireUser(userId);
        List<Address> addresses = addressesOf(user);
        if (addresses.size() >= MAX_ADDRESSES) {
            throw new IllegalArgumentException("You can save up to 8 delivery addresses.");
        }
        Address address = new Address();
        address.setId(UUID.randomUUID().toString());
        apply(address, request);
        addresses.add(address);
        if (addresses.size() == 1 || Boolean.TRUE.equals(request.defaultAddress())) {
            markDefault(addresses, address.getId());
        } else {
            normalizeDefaults(user);
        }
        return userRepository.save(user);
    }

    public User updateAddress(String userId, String addressId, AddressRequest request) {
        User user = requireUser(userId);
        Address address = requireAddress(user, addressId);
        apply(address, request);
        if (Boolean.TRUE.equals(request.defaultAddress())) {
            markDefault(addressesOf(user), addressId);
        } else {
            normalizeDefaults(user);
        }
        return userRepository.save(user);
    }

    public User deleteAddress(String userId, String addressId) {
        User user = requireUser(userId);
        List<Address> addresses = addressesOf(user);
        boolean removed = addresses.removeIf(address -> addressId.equals(address.getId()));
        if (!removed) {
            throw new IllegalArgumentException("Address not found.");
        }
        normalizeDefaults(user);
        return userRepository.save(user);
    }

    public User makeDefault(String userId, String addressId) {
        User user = requireUser(userId);
        requireAddress(user, addressId);
        markDefault(addressesOf(user), addressId);
        return userRepository.save(user);
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private static Address requireAddress(User user, String addressId) {
        return addressesOf(user).stream()
                .filter(address -> addressId.equals(address.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Address not found."));
    }

    private static List<Address> addressesOf(User user) {
        if (user.getAddresses() == null) {
            user.setAddresses(new ArrayList<>());
        } else if (!(user.getAddresses() instanceof ArrayList<?>)) {
            user.setAddresses(new ArrayList<>(user.getAddresses()));
        }
        return user.getAddresses();
    }

    private static void apply(Address address, AddressRequest request) {
        address.setLabel(requireLength(request.label(), 1, 40, "Enter a short label, such as Home or Work."));
        address.setRecipient(requireLength(request.recipient(), 2, 80, "Enter the recipient name."));
        address.setLine1(requireLength(request.line1(), 3, 120, "Enter the street address."));
        address.setLine2(optionalLength(request.line2(), 120, "Address line 2 is too long."));
        address.setCity(requireLength(request.city(), 2, 60, "Enter the city."));
        String state = requireLength(request.state(), 2, 60, "Choose a state or union territory.");
        if (!IndianStates.contains(state)) {
            throw new IllegalArgumentException("Choose a state or union territory.");
        }
        address.setState(state);
        String pin = request.postalCode() == null ? "" : request.postalCode().trim();
        if (!PIN.matcher(pin).matches()) {
            throw new IllegalArgumentException("Enter a 6-digit PIN code.");
        }
        address.setPostalCode(pin);
        address.setPhone(normalizePhone(request.phone()));
    }

    private static void markDefault(List<Address> addresses, String addressId) {
        for (Address address : addresses) {
            address.setDefaultAddress(addressId.equals(address.getId()));
        }
    }

    /**
     * @return true when a stored address had to be repaired
     */
    private static boolean normalizeDefaults(User user) {
        List<Address> addresses = user.getAddresses();
        if (addresses == null || addresses.isEmpty()) {
            return false;
        }
        long defaults = addresses.stream().filter(Address::isDefaultAddress).count();
        if (defaults == 1) {
            return false;
        }
        markDefault(addressesOf(user), addresses.get(0).getId());
        return true;
    }

    private static String normalizeAvatar(String avatar) {
        String trimmed = avatar.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException("Avatar link is too long.");
        }
        URI uri;
        try {
            uri = URI.create(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Avatar must be an http(s) link.");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))) {
            throw new IllegalArgumentException("Avatar must be an http(s) link.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Avatar must be an http(s) link.");
        }
        return trimmed;
    }

    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        if (!PHONE.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Enter a valid Indian mobile number.");
        }
        return trimmed;
    }

    private static String requireLength(String value, int min, int max, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        String trimmed = value.trim();
        if (trimmed.length() < min || trimmed.length() > max) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private static String optionalLength(String value, int max, String message) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }
}
