package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.identity.domain.SentNotice;
import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.SentNoticeRepository;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.service.mail.MailDispatcher;
import com.ecoorganicstore.identity.service.mail.OutboundMail;
import com.ecoorganicstore.identity.service.mail.StoreMessages;
import com.ecoorganicstore.identity.web.NotificationDtos.OrderLine;
import com.ecoorganicstore.identity.web.NotificationDtos.OrderNoticeRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderNoticeServiceTest {
    @Test
    void confirmedOrderIsMailedOnceAndARepeatIsIgnored() {
        User user = customer(true);
        List<OutboundMail> sent = new ArrayList<>();
        OrderNoticeService notices = service(user, sent, new HashSet<>());
        OrderNoticeRequest request = new OrderNoticeRequest(
                "u-1", "HC-1", "CONFIRMED", 25000, "12 Farm Road",
                List.of(new OrderLine("Spinach", 2, 12500)));

        notices.accept(request);
        notices.accept(request);

        assertEquals(1, sent.size());
        assertEquals("user@eco-organic-store.com", sent.get(0).to());
        assertEquals("admin@eco-organic-store.com", sent.get(0).bcc());
        assertTrue(sent.get(0).text().contains("₹250.00"));
    }

    @Test
    void optedOutCustomerStillNotifiesTheDesk() {
        User user = customer(false);
        List<OutboundMail> sent = new ArrayList<>();
        OrderNoticeService notices = service(user, sent, new HashSet<>());

        notices.accept(new OrderNoticeRequest("u-1", "HC-2", "PACKED", 100, "Lane", List.of()));

        assertEquals(1, sent.size());
        assertEquals("admin@eco-organic-store.com", sent.get(0).to());
        assertTrue(sent.get(0).subject().startsWith("[Desk] "));
    }

    @Test
    void unpaidOrdersAreNotMailed() {
        List<OutboundMail> sent = new ArrayList<>();
        OrderNoticeService notices = service(customer(true), sent, new HashSet<>());

        notices.accept(new OrderNoticeRequest("u-1", "HC-3", "PENDING_PAYMENT", 100, "Lane", List.of()));

        assertEquals(0, sent.size());
    }

    private static OrderNoticeService service(User user, List<OutboundMail> sent, Set<String> keys) {
        UserRepository users = mock(UserRepository.class);
        when(users.findById("u-1")).thenReturn(Optional.of(user));
        SentNoticeRepository notices = mock(SentNoticeRepository.class);
        when(notices.save(any(SentNotice.class))).thenAnswer(invocation -> {
            SentNotice notice = invocation.getArgument(0);
            if (!keys.add(notice.getDedupeKey())) {
                throw new DuplicateKeyException("duplicate");
            }
            return notice;
        });
        MailDispatcher dispatcher = new MailDispatcher(sent::add, Runnable::run);
        StoreMessages messages = new StoreMessages(
                "Marni eco organic store", "admin@eco-organic-store.com", "admin@eco-organic-store.com", "http://localhost:5173");
        return new OrderNoticeService(users, notices, dispatcher, messages);
    }

    private static User customer(boolean orderEmails) {
        User user = new User();
        user.setId("u-1");
        user.setEmail("user@eco-organic-store.com");
        user.setName("Asha");
        user.setEnabled(true);
        user.setOrderEmails(orderEmails);
        return user;
    }
}
