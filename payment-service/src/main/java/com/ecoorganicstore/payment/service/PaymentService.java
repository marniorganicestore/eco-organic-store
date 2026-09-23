package com.ecoorganicstore.payment.service;

import com.ecoorganicstore.payment.domain.Payment;
import com.ecoorganicstore.payment.domain.ProcessedEvent;
import com.ecoorganicstore.payment.repo.PaymentRepository;
import com.ecoorganicstore.payment.repo.ProcessedEventRepository;
import com.ecoorganicstore.common.web.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String internalKey;

    @Value("${app.stripe.secret-key:}")
    private String stripeSecret;
    @Value("${app.stripe.success-url:http://localhost:5173/order/success?session_id={CHECKOUT_SESSION_ID}}")
    private String successUrl;
    @Value("${app.stripe.cancel-url:http://localhost:5173/cart}")
    private String cancelUrl;
    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;
    @Value("${services.order:http://localhost:8085}")
    private String orderUrl;

    public PaymentService(PaymentRepository paymentRepository,
                          ProcessedEventRepository processedEventRepository,
                          RestClient restClient,
                          ObjectMapper objectMapper,
                          @Value("${app.internal-key}") String internalKey) {
        this.paymentRepository = paymentRepository;
        this.processedEventRepository = processedEventRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.internalKey = internalKey;
    }

    public SessionResponse createSession(String orderNumber, long amountPaise) {
        Payment payment = paymentRepository.findByOrderNumber(orderNumber).orElseGet(Payment::new);
        payment.setOrderNumber(orderNumber);
        payment.setAmountPaise(amountPaise);
        payment.setStatus("PENDING");
        String checkoutUrl;
        String sessionId;
        if (stripeSecret != null && !stripeSecret.isBlank()) {
            try {
                SessionCreateParams params = SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(successUrl)
                        .setCancelUrl(cancelUrl)
                        .setClientReferenceId(orderNumber)
                        .addLineItem(SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("inr")
                                        .setUnitAmount(amountPaise)
                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName("Marni Eco organic store Order " + orderNumber).build())
                                        .build())
                                .build())
                        .build();
                // One attempt, bounded. The SDK default (30s connect + 80s read, twice retried)
                // outlives the Azure ingress timeout and the browser then sees a header-less 504.
                RequestOptions options = RequestOptions.builder()
                        .setApiKey(stripeSecret)
                        .setConnectTimeout(10_000)
                        .setReadTimeout(20_000)
                        .setMaxNetworkRetries(0)
                        .build();
                Session session = Session.create(params, options);
                checkoutUrl = session.getUrl();
                sessionId = session.getId();
            } catch (StripeException e) {
                throw new IllegalArgumentException("Unable to create Stripe session");
            }
        } else {
            sessionId = "demo_" + UUID.randomUUID();
            checkoutUrl = "http://localhost:5173/order/success?orderNumber=" + orderNumber;
        }
        payment.setStripeSessionId(sessionId);
        payment = paymentRepository.save(payment);
        return new SessionResponse(payment.getId(), checkoutUrl);
    }

    public void handleWebhook(String eventId, String orderNumber, String status) {
        if (processedEventRepository.existsByEventId(eventId)) {
            return;
        }
        ProcessedEvent event = new ProcessedEvent();
        event.setEventId(eventId);
        processedEventRepository.save(event);

        Payment payment = paymentRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        payment.setStatus(status);
        paymentRepository.save(payment);
        if ("PAID".equals(status)) {
            restClient.post().uri(orderUrl + "/internal/orders/" + orderNumber + "/paid")
                    .header("X-Internal-Key", internalKey).contentType(MediaType.APPLICATION_JSON).retrieve().toBodilessEntity();
        }
    }

    public void handleWebhookPayload(String payload, String stripeSignature) {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (stripeSignature == null || stripeSignature.isBlank()) {
                throw new UnauthorizedException("Missing Stripe signature");
            }
            try {
                Event event = Webhook.constructEvent(payload, stripeSignature, webhookSecret);
                if ("checkout.session.completed".equals(event.getType())) {
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new IllegalArgumentException("Unsupported webhook event payload"));
                    handleWebhook(event.getId(), session.getClientReferenceId(), "PAID");
                } else if ("checkout.session.expired".equals(event.getType())) {
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new IllegalArgumentException("Unsupported webhook event payload"));
                    handleWebhook(event.getId(), session.getClientReferenceId(), "FAILED");
                }
            } catch (UnauthorizedException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new UnauthorizedException("Invalid Stripe signature");
            }
            return;
        }
        try {
            WebhookPayload webhookPayload = objectMapper.readValue(payload, WebhookPayload.class);
            handleWebhook(webhookPayload.eventId(), webhookPayload.orderNumber(), webhookPayload.status());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid webhook payload");
        }
    }

    public List<Payment> list() {
        return paymentRepository.findAll();
    }

    public record SessionResponse(String paymentId, String checkoutUrl) {}
    public record WebhookPayload(String eventId, String orderNumber, String status) {}
}