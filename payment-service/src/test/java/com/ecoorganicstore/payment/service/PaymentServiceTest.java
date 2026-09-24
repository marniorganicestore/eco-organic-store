package com.ecoorganicstore.payment.service;

import com.ecoorganicstore.common.web.UnauthorizedException;
import com.ecoorganicstore.payment.domain.Payment;
import com.ecoorganicstore.payment.repo.PaymentRepository;
import com.ecoorganicstore.payment.repo.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    private PaymentRepository paymentRepository;
    private ProcessedEventRepository processedEventRepository;
    private RestClient restClient;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec body = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        when(restClient.post()).thenReturn(body);
        when(body.uri(org.mockito.ArgumentMatchers.anyString())).thenReturn(body);
        when(body.contentType(any())).thenReturn(body);
        when(body.header(any(), org.mockito.ArgumentMatchers.<String>any())).thenReturn(body);
        when(body.retrieve()).thenReturn(response);
        service = new PaymentService(
                paymentRepository,
                processedEventRepository,
                restClient,
                new ObjectMapper(),
                "internal-key",
                "rzp_test_key",
                "key-secret",
                "whsec_test",
                "http://localhost:8080/api/payments/razorpay/callback",
                "http://localhost:8085");
    }

    @Test
    void rejectsMissingSignatureWhenSecretConfigured() {
        assertThrows(UnauthorizedException.class, () -> service.handleWebhookPayload("{\"event\":\"payment_link.paid\"}", null, "evt_1"));
    }

    @Test
    void rejectsInvalidSignature() {
        assertThrows(UnauthorizedException.class, () -> service.handleWebhookPayload("{\"event\":\"payment_link.paid\"}", "deadbeef", "evt_1"));
    }

    @Test
    void sameEventIdIsAppliedOnce() throws Exception {
        String payload = """
                {"event":"payment_link.paid","created_at":1,"payload":{"payment_link":{"entity":{"id":"plink_1","reference_id":"HC-1","status":"paid"}}}}
                """;
        Payment payment = new Payment();
        payment.setOrderNumber("HC-1");
        payment.setStatus("PENDING");
        when(paymentRepository.findByOrderNumber("HC-1")).thenReturn(Optional.of(payment));
        when(processedEventRepository.existsByEventId("evt_1")).thenReturn(false, true);

        String signature = hmac(payload, "whsec_test");
        service.handleWebhookPayload(payload, signature, "evt_1");
        service.handleWebhookPayload(payload, signature, "evt_1");

        verify(restClient, times(1)).post();
        verify(processedEventRepository, times(1)).save(any());
    }

    @Test
    void razorpayErrorDescriptionIsShownWithoutTheRawBody() {
        String body = "{\"error\":{\"description\":\"Authentication failed\"}}";
        assertEquals("Authentication failed", PaymentService.razorpayDescription(body));
        assertEquals("Unable to create Razorpay payment link. Authentication failed",
                PaymentService.paymentLinkFailure(PaymentService.razorpayDescription(body)));
        assertEquals("Unable to create Razorpay payment link", PaymentService.paymentLinkFailure(""));
    }

    @Test
    void callbackRequiresKeySecretSignature() throws Exception {
        assertFalse(service.confirmCallback("plink_1", "HC-1", "paid", "pay_1", "nope"));
        String signature = hmac("plink_1|HC-1|paid|pay_1", "key-secret");
        Payment payment = new Payment();
        payment.setOrderNumber("HC-1");
        payment.setStatus("PENDING");
        when(paymentRepository.findByOrderNumber("HC-1")).thenReturn(Optional.of(payment));
        when(processedEventRepository.existsByEventId("callback:pay_1")).thenReturn(false);
        assertTrue(service.confirmCallback("plink_1", "HC-1", "paid", "pay_1", signature));
    }

    private static String hmac(String message, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    }
}
