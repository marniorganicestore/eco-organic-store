package com.ecoorganicstore.identity.service;

import com.ecoorganicstore.identity.domain.SentNotice;
import com.ecoorganicstore.identity.domain.User;
import com.ecoorganicstore.identity.repo.SentNoticeRepository;
import com.ecoorganicstore.identity.repo.UserRepository;
import com.ecoorganicstore.identity.service.mail.MailDispatcher;
import com.ecoorganicstore.identity.service.mail.OrderMailCommand;
import com.ecoorganicstore.identity.service.mail.StoreMessages;
import com.ecoorganicstore.identity.web.NotificationDtos.OrderNoticeRequest;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class OrderNoticeService {
    private static final Logger log = LoggerFactory.getLogger(OrderNoticeService.class);
    private static final Set<String> MAILED = Set.of("CONFIRMED", "PACKED", "SHIPPED", "DELIVERED");

    private final UserRepository users;
    private final SentNoticeRepository notices;
    private final MailDispatcher dispatcher;
    private final StoreMessages messages;

    public OrderNoticeService(
            UserRepository users,
            SentNoticeRepository notices,
            MailDispatcher dispatcher,
            StoreMessages messages) {
        this.users = users;
        this.notices = notices;
        this.dispatcher = dispatcher;
        this.messages = messages;
    }

    public void accept(OrderNoticeRequest request) {
        String status = request.orderStatus() == null ? "" : request.orderStatus().trim().toUpperCase(Locale.ROOT);
        if (!MAILED.contains(status)) {
            return;
        }
        String key = status + "|" + request.orderNumber().trim();
        SentNotice notice = new SentNotice();
        notice.setDedupeKey(key);
        try {
            notices.save(notice);
        } catch (DuplicateKeyException ex) {
            log.info("Order email already queued for {}", key);
            return;
        }
        User user = users.findById(request.userId()).orElse(null);
        List<OrderMailCommand.Line> lines = request.lines() == null
                ? List.of()
                : request.lines().stream()
                        .map(line -> new OrderMailCommand.Line(line.productName(), line.qty(), line.pricePaise()))
                        .toList();
        OrderMailCommand command = new OrderMailCommand(
                user == null ? "" : user.getName(),
                user == null || !user.isEnabled() ? "" : user.getEmail(),
                user != null && user.isEnabled() && user.wantsOrderEmail(),
                request.orderNumber().trim(),
                status,
                request.totalPaise(),
                request.shippingAddress(),
                lines);
        try {
            dispatcher.dispatch(messages.order(command));
        } catch (RuntimeException ex) {
            log.warn("Order email {} was not queued: {}", key, ex.toString());
        }
    }
}
