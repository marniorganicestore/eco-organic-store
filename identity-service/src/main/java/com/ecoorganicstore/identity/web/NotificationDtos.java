package com.ecoorganicstore.identity.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record NotificationPreferencesResponse(boolean orderUpdates, String email, String fromAddress) {}

    public record UpdateNotificationPreferencesRequest(@NotNull Boolean orderUpdates) {}

    public record OrderNoticeRequest(
            @NotBlank String userId,
            @NotBlank String orderNumber,
            @NotBlank String orderStatus,
            long totalPaise,
            String shippingAddress,
            @Valid List<OrderLine> lines) {}

    public record OrderLine(String productName, int qty, long pricePaise) {}
}
