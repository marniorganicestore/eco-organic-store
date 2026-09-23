package com.ecoorganicstore.identity.service.mail;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreMessagesTest {
    private final StoreMessages messages = new StoreMessages(
            "Marni eco organic store", "admin@eco-organic-store.com", "admin@eco-organic-store.com", "https://eco-organic-store.com/");

    @Test
    void orderMailGoesToTheCustomerAndCopiesTheStore() {
        OutboundMail mail = messages.order(new OrderMailCommand(
                "Ada <script>",
                "ada@example.com",
                true,
                "HC-AB12",
                "CONFIRMED",
                17900,
                "12 Farm Road",
                List.of(new OrderMailCommand.Line("Organic honey", 1, 17900))));

        assertEquals("ada@example.com", mail.to());
        assertEquals("admin@eco-organic-store.com", mail.bcc());
        assertEquals("Order HC-AB12 is confirmed", mail.subject());
        assertTrue(mail.text().contains("₹179.00"));
        assertTrue(mail.html().contains("Ada &lt;script&gt;"));
        assertFalse(mail.html().contains("<script>"));
        assertTrue(mail.html().contains("https://eco-organic-store.com/brand/logo-email.png"));
        assertTrue(mail.html().contains("admin@eco-organic-store.com"));
    }

    @Test
    void optedOutOrdersStayWithTheStoreDesk() {
        OutboundMail mail = messages.order(new OrderMailCommand(
                "Ada", "ada@example.com", false, "HC-AB12", "SHIPPED", 5000, "12 Farm Road", List.of()));

        assertEquals("admin@eco-organic-store.com", mail.to());
        assertEquals(null, mail.bcc());
        assertTrue(mail.subject().startsWith("[Desk] "));
        assertTrue(mail.text().contains("on the way"));
    }

    @Test
    void resetLinkUsesTheStorefront() {
        OutboundMail mail = messages.passwordReset("Ada", "ada@example.com", "https://eco-organic-store.com/forgot-password?token=abc");

        assertEquals("ada@example.com", mail.to());
        assertEquals(null, mail.bcc());
        assertTrue(mail.text().contains("https://eco-organic-store.com/forgot-password?token=abc"));
        assertEquals("https://eco-organic-store.com", messages.storefrontUrl());
    }
}
