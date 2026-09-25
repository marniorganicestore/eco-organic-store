package com.ecoorganicstore.payment.web;

import com.ecoorganicstore.common.security.AuthGuards;
import com.ecoorganicstore.common.web.PageResponse;
import com.ecoorganicstore.payment.domain.Payment;
import com.ecoorganicstore.payment.service.PaymentService;
import com.ecoorganicstore.payment.service.PaymentService.SessionResponse;
import com.ecoorganicstore.payment.service.PaymentService.VerifyResponse;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class PaymentController {
    private final PaymentService paymentService;
    private final String successUrl;
    private final String cancelUrl;

    public PaymentController(PaymentService paymentService,
                             @Value("${app.razorpay.success-url:http://localhost:5173/order/success}") String successUrl,
                             @Value("${app.razorpay.cancel-url:http://localhost:5173/cart}") String cancelUrl) {
        this.paymentService = paymentService;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    @PostMapping("/internal/payments/session")
    public SessionResponse createSession(@RequestBody SessionRequest request) {
        return paymentService.createSession(request.orderNumber(), request.amountPaise());
    }

    @PostMapping("/api/payments/verify")
    public VerifyResponse verify(HttpServletRequest request, @RequestBody VerifyRequest body) {
        AuthGuards.requireUser(request);
        return paymentService.verifyPayment(body.razorpayOrderId(), body.razorpayPaymentId(), body.razorpaySignature());
    }

    @PostMapping("/api/webhooks/razorpay")
    public void webhook(@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                        @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId,
                        @RequestBody String payload) {
        paymentService.handleWebhookPayload(payload, signature, eventId);
    }

    @GetMapping("/api/payments/razorpay/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(value = "razorpay_payment_link_id", required = false) String paymentLinkId,
            @RequestParam(value = "razorpay_payment_link_reference_id", required = false) String referenceId,
            @RequestParam(value = "razorpay_payment_link_status", required = false) String status,
            @RequestParam(value = "razorpay_payment_id", required = false) String paymentId,
            @RequestParam(value = "razorpay_signature", required = false) String signature) {
        boolean paid = paymentService.confirmCallback(paymentLinkId, referenceId, status, paymentId, signature);
        URI target = paymentService.redirectAfterCallback(paid, successUrl, cancelUrl);
        return ResponseEntity.status(302).location(target).build();
    }

    @GetMapping("/api/admin/payments/summary")
    public PaymentDeskResponse paymentSummary(HttpServletRequest request) {
        AuthGuards.requireAdmin(request);
        return new PaymentDeskResponse(paymentService.pendingCount());
    }

    @GetMapping("/api/admin/payments")
    public PageResponse<PaymentView> payments(HttpServletRequest request,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        AuthGuards.requireAdmin(request);
        var result = paymentService.list(page, size);
        List<PaymentView> items = result.getContent().stream().map(PaymentController::toView).toList();
        return PageResponse.of(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    private static PaymentView toView(Payment payment) {
        return new PaymentView(
                payment.getId(),
                payment.getOrderNumber(),
                payment.getAmountPaise(),
                payment.getStatus(),
                payment.getCreatedAt());
    }

    public record PaymentDeskResponse(long pendingCount) {}
    public record SessionRequest(String orderNumber, long amountPaise) {}

    public record VerifyRequest(
            @JsonProperty("razorpay_order_id") @JsonAlias("razorpayOrderId") String razorpayOrderId,
            @JsonProperty("razorpay_payment_id") @JsonAlias("razorpayPaymentId") String razorpayPaymentId,
            @JsonProperty("razorpay_signature") @JsonAlias("razorpaySignature") String razorpaySignature) {}
    public record PaymentView(String id, String orderNumber, long amountPaise, String status, Instant createdAt) {}
}
