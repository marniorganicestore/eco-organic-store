package com.ecoorganicstore.payment.service;

import com.ecoorganicstore.common.web.UnauthorizedException;
import com.ecoorganicstore.payment.domain.Payment;
import com.ecoorganicstore.payment.domain.ProcessedEvent;
import com.ecoorganicstore.payment.repo.PaymentRepository;
import com.ecoorganicstore.payment.repo.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class PaymentService {
    private static final String RAZORPAY_PAYMENT_LINKS = "https://api.razorpay.com/v1/payment_links";
    private static final long LINK_TTL_SECONDS = 20 * 60;

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String internalKey;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final String callbackUrl;
    private final String orderUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          ProcessedEventRepository processedEventRepository,
                          RestClient restClient,
                          ObjectMapper objectMapper,
                          @Value("${app.internal-key}") String internalKey,
                          @Value("${app.razorpay.key-id:}") String keyId,
                          @Value("${app.razorpay.key-secret:}") String keySecret,
                          @Value("${app.razorpay.webhook-secret:}") String webhookSecret,
                          @Value("${app.razorpay.callback-url:http://localhost:8080/api/payments/razorpay/callback}") String callbackUrl,
                          @Value("${services.order:http://localhost:8085}") String orderUrl) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.internalKey = internalKey;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.callbackUrl = callbackUrl;
        this.orderUrl = orderUrl;
    }

    public SessionResponse createSession(String orderNumber, long amountPaise) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber).orElseGet(Payment::new);
        payment.setOrderNumber(orderNumber);
        payment.setAmountPaise(amountPaise);
        payment.setStatus("PENDING");
        String checkoutUrl;
        String paymentLinkId;
        if (razorpayConfigured()) {
            if (amountPaise < 100) {
                throw new IllegalArgumentException("Razorpay requires a minimum charge of ₹1.");
            }
            try {
                PaymentLinkResponse link = restClient.post()
                        .uri(RAZORPAY_PAYMENT_LINKS)
                        .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "amount", amountPaise,
                                "currency", "INR",
                                "accept_partial", false,
                                "description", "Marni Eco organic store Order " + orderNumber,
                                "reference_id", orderNumber,
                                "callback_url", callbackUrl,
                                "callback_method", "get",
                                "expire_by", Instant.now().plusSeconds(LINK_TTL_SECONDS).getEpochSecond(),
                                "reminder_enable", false,
                                "notify", Map.of("sms", false, "email", false),
                                "notes", Map.of("order_number", orderNumber)))
                        .retrieve()
                        .body(PaymentLinkResponse.class);
                if (link == null || link.short_url() == null || link.short_url().isBlank() || link.id() == null) {
                    throw new IllegalArgumentException("Unable to create Razorpay payment link");
                }
                checkoutUrl = link.short_url();
                paymentLinkId = link.id();
            } catch (RestClientException ex) {
                throw new IllegalArgumentException("Unable to create Razorpay payment link");
            }
        } else {
            paymentLinkId = "demo_" + UUID.randomUUID();
            checkoutUrl = "http://localhost:5173/order/success?orderNumber=" + orderNumber;
        }
        payment.setPaymentLinkId(paymentLinkId);
        payment = paymentRepository.save(payment);
        return new SessionResponse(payment.getId(), checkoutUrl);
    }

    public void handleWebhookPayload(String payload, String signature, String eventId) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new UnauthorizedException("Missing Razorpay webhook secret");
        }
        if (!signaturesMatch(payload, signature, webhookSecret)) {
            throw new UnauthorizedException("Invalid Razorpay signature");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("event").asText("");
            JsonNode link = root.path("payload").path("payment_link").path("entity");
            String orderNumber = link.path("reference_id").asText("");
            String resolvedEventId = eventId == null || eventId.isBlank()
                    ? type + ":" + link.path("id").asText("") + ":" + root.path("created_at").asText("")
                    : eventId;
            if ("payment_link.paid".equals(type)) {
                applyOutcome(resolvedEventId, orderNumber, "PAID");
            } else if ("payment_link.expired".equals(type) || "payment_link.cancelled".equals(type)) {
                applyOutcome(resolvedEventId, orderNumber, "FAILED");
            }
        } catch (JsonProcessingException ex) {
            throw new UnauthorizedException("Invalid Razorpay signature");
        }
    }

    public boolean confirmCallback(String paymentLinkId, String referenceId, String status, String paymentId, String signature) {
        if (!razorpayConfigured()) {
            return false;
        }
        String message = paymentLinkId + "|" + referenceId + "|" + status + "|" + paymentId;
        if (!signaturesMatch(message, signature, keySecret)) {
            return false;
        }
        if (!"paid".equalsIgnoreCase(status)) {
            return false;
        }
        applyOutcome("callback:" + paymentId, referenceId, "PAID");
        return true;
    }

    public URI redirectAfterCallback(boolean paid, String successUrl, String cancelUrl) {
        return URI.create(paid ? successUrl : cancelUrl);
    }

    public List<Payment> list() {
        return paymentRepository.findAll();
    }

    private void applyOutcome(String eventId, String orderNumber, String status) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Payment not found");
        }
        if (processedEventRepository.existsByEventId(eventId)) {
            return;
        }
        ProcessedEvent event = new ProcessedEvent();
        event.setEventId(eventId);
        processedEventRepository.save(event);

        Payment payment = paymentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if ("PAID".equals(payment.getStatus())) {
            return;
        }
        payment.setStatus(status);
        paymentRepository.save(payment);
        if ("PAID".equals(status)) {
            restClient.post().uri(orderUrl + "/internal/orders/" + orderNumber + "/paid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Key", internalKey)
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    private boolean razorpayConfigured() {
        return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
    }

    static boolean signaturesMatch(String message, String signature, String secret) {
        if (message == null || signature == null || signature.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
            byte[] actual = signature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual);
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }

    public record SessionResponse(String paymentId, String checkoutUrl) {}

    public record PaymentLinkResponse(String id, String short_url, String status) {}
}
