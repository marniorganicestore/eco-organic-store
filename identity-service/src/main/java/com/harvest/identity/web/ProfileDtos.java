package com.harvest.identity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ProfileDtos {
    private ProfileDtos() {}

    public record UpdateProfileRequest(
            @Size(min = 2, max = 80, message = "Name must be 2–80 characters.") String name,
            @Size(max = 1000, message = "Avatar link is too long.") String avatar,
            @Size(max = 16, message = "Enter a valid Indian mobile number.") String phone) {}

    public record AddressRequest(
            @NotBlank(message = "Enter a short label, such as Home or Work.") @Size(max = 40) String label,
            @NotBlank(message = "Enter the recipient name.") @Size(max = 80) String recipient,
            @NotBlank(message = "Enter the street address.") @Size(max = 120) String line1,
            @Size(max = 120, message = "Address line 2 is too long.") String line2,
            @NotBlank(message = "Enter the city.") @Size(max = 60) String city,
            @NotBlank(message = "Choose a state or union territory.") @Size(max = 60) String state,
            @NotBlank(message = "Enter a 6-digit PIN code.") @Pattern(regexp = "[1-9][0-9]{5}", message = "Enter a 6-digit PIN code.") String postalCode,
            @Size(max = 16) String phone,
            Boolean defaultAddress) {}

    public record AddressResponse(
            String id,
            String label,
            String recipient,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String phone,
            boolean defaultAddress) {}

    public record ProfileResponse(
            String userId,
            String email,
            String name,
            String avatar,
            String phone,
            List<String> roles,
            boolean passwordSet,
            boolean googleLinked,
            List<AddressResponse> addresses) {}
}
