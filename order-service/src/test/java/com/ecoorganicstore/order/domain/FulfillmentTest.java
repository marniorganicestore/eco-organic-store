package com.ecoorganicstore.order.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FulfillmentTest {
    @Test
    void confirmedOrdersAdvanceOnlyToPacked() {
        assertEquals("PACKED", Fulfillment.advance("CONFIRMED", "packed"));
        assertEquals("Pack this order before it can ship.",
                assertThrows(IllegalArgumentException.class, () -> Fulfillment.advance("CONFIRMED", "DELIVERED")).getMessage());
    }

    @Test
    void unpaidOrdersStayWithThePaymentSaga() {
        assertEquals("This order is still waiting for payment.",
                assertThrows(IllegalArgumentException.class, () -> Fulfillment.advance("PENDING_PAYMENT", "PACKED")).getMessage());
    }
}
