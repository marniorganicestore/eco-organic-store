package com.ecoorganicstore.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecoorganicstore.common.web.UnauthorizedException;
import com.ecoorganicstore.payment.repo.PaymentRepository;
import com.ecoorganicstore.payment.repo.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class PaymentServiceTest {
    @Test
    void rejectsMissingStripeSignatureWhenSecretConfigured() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        ProcessedEventRepository processedEventRepository = mock(ProcessedEventRepository.class);
        RestClient restClient = mock(RestClient.class);
        PaymentService service = new PaymentService(
                paymentRepository,
                processedEventRepository,
                restClient,
                new ObjectMapper(),
                "internal-key");
        ReflectionTestUtils.setField(service, "webhookSecret", "whsec_test");

        assertThrows(UnauthorizedException.class, () -> service.handleWebhookPayload("{\"id\":\"evt_1\"}", null));
    }
}
