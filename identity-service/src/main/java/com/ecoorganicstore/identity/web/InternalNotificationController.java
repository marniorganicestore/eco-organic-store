package com.ecoorganicstore.identity.web;

import com.ecoorganicstore.identity.service.OrderNoticeService;
import com.ecoorganicstore.identity.web.NotificationDtos.OrderNoticeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
public class InternalNotificationController {
    private final OrderNoticeService notices;

    public InternalNotificationController(OrderNoticeService notices) {
        this.notices = notices;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void order(@Valid @RequestBody OrderNoticeRequest request) {
        notices.accept(request);
    }
}
