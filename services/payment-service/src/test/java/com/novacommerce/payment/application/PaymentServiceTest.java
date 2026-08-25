package com.novacommerce.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novacommerce.payment.api.PaymentDtos.*;
import com.novacommerce.payment.api.error.PaymentException;
import com.novacommerce.payment.config.PaymentProperties;
import com.novacommerce.payment.domain.*;
import com.novacommerce.payment.infrastructure.gateway.MockPaymentGateway;
import com.novacommerce.payment.infrastructure.outbox.OutboxEvent;
import com.novacommerce.payment.infrastructure.outbox.OutboxRepository;
import com.novacommerce.payment.infrastructure.persistence.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    private PaymentService paymentService;
    private MockPaymentGateway mockGateway;

    @BeforeEach
    void setUp() {
        mockGateway = new MockPaymentGateway();
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("MOCK");
        ObjectMapper objectMapper = new ObjectMapper();

        paymentService = new PaymentService(
                paymentRepository,
                outboxRepository,
                List.of(mockGateway),
                properties,
                objectMapper
        );
    }

    @Test
    void authorizePayment_Success() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String idempotencyKey = "idempotency-123";

        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                orderId,
                new BigDecimal("99.99"),
                "USD",
                idempotencyKey,
                "tok_valid"
        );

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.authorizePayment(customerId, request);

        assertNotNull(response);
        assertEquals(orderId, response.orderId());
        assertEquals(customerId, response.customerId());
        assertEquals(new BigDecimal("99.99"), response.amount());
        assertEquals("AUTHORIZED", response.status());
        assertNotNull(response.gatewayTransactionId());

        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void authorizePayment_IdempotentDuplicate() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String idempotencyKey = "idempotency-123";

        Payment existingPayment = Payment.createPending(
                paymentId, orderId, customerId, new BigDecimal("99.99"), "USD", idempotencyKey, "MOCK"
        );
        existingPayment.authorize("mock_tx_123");

        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                orderId, new BigDecimal("99.99"), "USD", idempotencyKey, "tok_valid"
        );

        PaymentResponse response = paymentService.authorizePayment(customerId, request);

        assertEquals(paymentId, response.id());
        assertEquals("AUTHORIZED", response.status());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void authorizePayment_DuplicatePaymentAttemptOnPaidOrder() {
        UUID customerId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Payment existingPayment = Payment.createPending(
                UUID.randomUUID(), orderId, customerId, new BigDecimal("99.99"), "USD", "key-1", "MOCK"
        );
        existingPayment.authorize("mock_tx_1");

        when(paymentRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(paymentRepository.findByOrderId(orderId)).thenReturn(List.of(existingPayment));

        AuthorizePaymentRequest request = new AuthorizePaymentRequest(
                orderId, new BigDecimal("99.99"), "USD", "key-2", "tok_valid"
        );

        PaymentException ex = assertThrows(PaymentException.class,
                () -> paymentService.authorizePayment(customerId, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already been paid or authorized"));
    }

    @Test
    void capturePayment_Success() {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Payment payment = Payment.createPending(
                paymentId, orderId, customerId, new BigDecimal("50.00"), "USD", "key-cap", "MOCK"
        );
        payment.authorize("mock_tx_auth");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.capturePayment(paymentId, new BigDecimal("50.00"));

        assertEquals("CAPTURED", response.status());
        verify(outboxRepository, times(1)).save(any(OutboxEvent.class));
    }
}
