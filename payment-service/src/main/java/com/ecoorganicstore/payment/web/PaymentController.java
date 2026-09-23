package com.ecoorganicstore.payment.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.payment.domain.Payment;
import com.ecoorganicstore.payment.service.PaymentService;
import com.ecoorganicstore.payment.service.PaymentService.SessionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/internal/payments/session")
    public SessionResponse createSession(@RequestBody SessionRequest request) {
        return paymentService.createSession(request.orderNumber(), request.amountPaise());
    }

    @PostMapping("/api/webhooks/stripe")
    public void webhook(@RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
                        @RequestBody String payload) {
        paymentService.handleWebhookPayload(payload, stripeSignature);
    }

    @GetMapping("/api/admin/payments")
    public List<Payment> payments(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
        return paymentService.list();
    }

    public record SessionRequest(String orderNumber, long amountPaise) {}
}