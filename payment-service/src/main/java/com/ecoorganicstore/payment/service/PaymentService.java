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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String RAZORPAY_ORDERS = "https://api.razorpay.com/v1/orders";
    private static final long MINIMUM_AMOUNT_PAISE = 100;

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String internalKey;
    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final String orderUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          ProcessedEventRepository processedEventRepository,
                          RestClient restClient,
                          ObjectMapper objectMapper,
                          @Value("${app.internal-key}") String internalKey,
                          @Value("${app.razorpay.key-id:}") String keyId,
                          @Value("${app.razorpay.key-secret:}") String keySecret,
                          @Value("${app.razorpay.webhook-secret:}") String webhookSecret,
                          @Value("${services.order:http://localhost:8085}") String orderUrl) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.internalKey = internalKey;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.orderUrl = orderUrl;
    }

    public SessionResponse createSession(String orderNumber, long amountPaise) {
        if (amountPaise < MINIMUM_AMOUNT_PAISE) {
            throw new IllegalArgumentException("Razorpay requires a minimum charge of ₹1.");
        }
        Payment payment = paymentRepository.findByOrderNumber(orderNumber).orElseGet(Payment::new);
        payment.setOrderNumber(orderNumber);
        payment.setAmountPaise(amountPaise);
        payment.setStatus("PENDING");
        if (!razorpayConfigured()) {
            payment.setPaymentLinkId("demo_" + UUID.randomUUID());
            payment = paymentRepository.save(payment);
            return new SessionResponse(
                    payment.getId(),
                    "",
                    amountPaise,
                    "INR",
                    "",
                    "http://localhost:5173/order/success?orderNumber=" + orderNumber);
        }
        try {
            String raw = restClient.post()
                    .uri(RAZORPAY_ORDERS)
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "amount", amountPaise,
                            "currency", "INR",
                            "receipt", orderNumber,
                            "notes", Map.of("order_number", orderNumber)))
                    .retrieve()
                    .body(String.class);
            JsonNode order = raw == null ? null : objectMapper.readTree(raw);
            String razorpayOrderId = order == null ? "" : order.path("id").asText("");
            long amount = order == null ? 0 : order.path("amount").asLong(0);
            String currency = order == null ? "" : order.path("currency").asText("");
            if (razorpayOrderId.isBlank() || amount < MINIMUM_AMOUNT_PAISE || currency.isBlank()) {
                throw new IllegalStateException("Unable to create Razorpay order");
            }
            payment.setRazorpayOrderId(razorpayOrderId);
            payment = paymentRepository.save(payment);
            return new SessionResponse(payment.getId(), razorpayOrderId, amount, currency, keyId, "");
        } catch (RestClientResponseException ex) {
            throw razorpayOrderFailure(ex);
        } catch (RestClientException | JsonProcessingException ex) {
            log.warn("Razorpay order failed", ex);
            throw new IllegalStateException("Unable to create Razorpay order", ex);
        }
    }

    public VerifyResponse verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        if (isBlank(razorpayOrderId) || isBlank(razorpayPaymentId) || isBlank(razorpaySignature)) {
            throw new IllegalArgumentException("Payment confirmation is incomplete.");
        }
        if (!razorpayConfigured()) {
            throw new IllegalStateException("Razorpay is not configured.");
        }
        String message = razorpayOrderId.trim() + "|" + razorpayPaymentId.trim();
        if (!signaturesMatch(message, razorpaySignature, keySecret)) {
            throw new IllegalArgumentException("Payment signature does not match.");
        }
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        applyOutcome("checkout:" + razorpayPaymentId.trim(), payment.getOrderNumber(), "PAID");
        return new VerifyResponse(true, payment.getOrderNumber());
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
            } else if ("order.paid".equals(type) || "payment.captured".equals(type)) {
                String storeOrder = orderNumberFromStandardEvent(root);
                String paymentId = root.path("payload").path("payment").path("entity").path("id").asText("");
                String standardEventId = eventId == null || eventId.isBlank() ? type + ":" + paymentId : eventId;
                if (!storeOrder.isBlank()) {
                    applyOutcome(standardEventId, storeOrder, "PAID");
                }
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

    static String razorpayDescription(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode error = new ObjectMapper().readTree(body).path("error").path("description");
            String description = error.asText("").trim();
            if (description.length() > 180) {
                description = description.substring(0, 180);
            }
            return description;
        } catch (JsonProcessingException ex) {
            return "";
        }
    }

    static String paymentLinkFailure(String description) {
        if (description == null || description.isBlank()) {
            return "Unable to create Razorpay order";
        }
        return "Unable to create Razorpay order. " + description;
    }

    private RuntimeException razorpayOrderFailure(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String description = razorpayDescription(ex.getResponseBodyAsString());
        log.warn("Razorpay order failed: status={} description={}", status, description);
        if (status == 401 || status == 403) {
            return new UnauthorizedException("Razorpay authentication failed");
        }
        return new IllegalStateException(paymentLinkFailure(description), ex);
    }

    private String orderNumberFromStandardEvent(JsonNode root) {
        JsonNode payment = root.path("payload").path("payment").path("entity");
        String fromNotes = payment.path("notes").path("order_number").asText("");
        if (!fromNotes.isBlank()) {
            return fromNotes;
        }
        JsonNode order = root.path("payload").path("order").path("entity");
        String receipt = order.path("receipt").asText("");
        if (!receipt.isBlank()) {
            return receipt;
        }
        String fromOrderNotes = order.path("notes").path("order_number").asText("");
        if (!fromOrderNotes.isBlank()) {
            return fromOrderNotes;
        }
        String razorpayOrderId = payment.path("order_id").asText(order.path("id").asText(""));
        if (razorpayOrderId.isBlank()) {
            return "";
        }
        return paymentRepository.findByRazorpayOrderId(razorpayOrderId).map(Payment::getOrderNumber).orElse("");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record SessionResponse(String paymentId, String orderId, long amount, String currency, String keyId, String checkoutUrl) {}

    public record VerifyResponse(boolean success, String orderNumber) {}
}
