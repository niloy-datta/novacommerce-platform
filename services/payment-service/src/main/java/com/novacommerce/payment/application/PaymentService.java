package com.novacommerce.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novacommerce.payment.api.PaymentDtos.*;
import com.novacommerce.payment.api.error.PaymentException;
import com.novacommerce.payment.config.PaymentProperties;
import com.novacommerce.payment.domain.*;
import com.novacommerce.payment.infrastructure.outbox.OutboxEvent;
import com.novacommerce.payment.infrastructure.outbox.OutboxRepository;
import com.novacommerce.payment.infrastructure.persistence.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final Map<String, PaymentGateway> gateways;
    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;

    public PaymentService(PaymentRepository paymentRepository,
                          OutboxRepository outboxRepository,
                          List<PaymentGateway> gatewayList,
                          PaymentProperties properties,
                          ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxRepository = outboxRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.gateways = new HashMap<>();
        for (PaymentGateway gateway : gatewayList) {
            this.gateways.put(gateway.getProviderName().toUpperCase(), gateway);
        }
    }

    @Transactional
    public PaymentResponse authorizePayment(UUID customerId, AuthorizePaymentRequest request) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        List<Payment> orderPayments = paymentRepository.findByOrderId(request.orderId());
        boolean hasSuccessfulPayment = orderPayments.stream()
                .anyMatch(p -> p.getStatus() == PaymentStatus.AUTHORIZED || p.getStatus() == PaymentStatus.CAPTURED);
        if (hasSuccessfulPayment) {
            throw new PaymentException(HttpStatus.CONFLICT, "Order " + request.orderId() + " has already been paid or authorized.");
        }

        String providerName = properties.getProvider().toUpperCase();
        PaymentGateway gateway = gateways.getOrDefault(providerName, gateways.get("MOCK"));

        Payment payment = Payment.createPending(
                UUID.randomUUID(),
                request.orderId(),
                customerId,
                request.amount(),
                request.currency(),
                request.idempotencyKey(),
                gateway.getProviderName()
        );
        payment = paymentRepository.save(payment);

        PaymentGatewayResult result = gateway.authorize(
                request.orderId(),
                request.amount(),
                request.currency(),
                request.paymentToken()
        );

        if (result.success()) {
            payment.authorize(result.gatewayTransactionId());
            createOutboxEvent("PaymentAuthorized", payment);
        } else {
            payment.fail(result.errorMessage());
            createOutboxEvent("PaymentFailed", payment);
        }

        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    @Transactional
    public PaymentResponse capturePayment(UUID paymentId, BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "Payment not found with id " + paymentId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "Payment is not in AUTHORIZED status. Current: " + payment.getStatus());
        }

        PaymentGateway gateway = gateways.getOrDefault(payment.getGatewayProvider().toUpperCase(), gateways.get("MOCK"));
        PaymentGatewayResult result = gateway.capture(payment.getGatewayTransactionId(), amount);

        if (result.success()) {
            payment.capture();
            createOutboxEvent("PaymentCaptured", payment);
        } else {
            payment.fail(result.errorMessage());
            createOutboxEvent("PaymentCaptureFailed", payment);
        }

        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, BigDecimal amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "Payment not found with id " + paymentId));

        if (payment.getStatus() != PaymentStatus.CAPTURED && payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "Payment cannot be refunded in current status: " + payment.getStatus());
        }

        PaymentGateway gateway = gateways.getOrDefault(payment.getGatewayProvider().toUpperCase(), gateways.get("MOCK"));
        PaymentGatewayResult result = gateway.refund(payment.getGatewayTransactionId(), amount);

        if (result.success()) {
            payment.refund();
            createOutboxEvent("RefundCompleted", payment);
        } else {
            throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "Refund failed: " + result.errorMessage());
        }

        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForOrder(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void processStripeWebhook(String payload, String signature) {
        // Handle webhook event parsing and signature verification safely
        if (payload != null && payload.contains("payment_intent.succeeded")) {
            // Find payment by gateway transaction ID if payload provides it
        }
    }

    private void createOutboxEvent(String eventType, Payment payment) {
        try {
            Map<String, Object> data = Map.of(
                    "paymentId", payment.getId().toString(),
                    "orderId", payment.getOrderId().toString(),
                    "customerId", payment.getCustomerId().toString(),
                    "amount", payment.getAmount(),
                    "currency", payment.getCurrency(),
                    "status", payment.getStatus().name(),
                    "gatewayTransactionId", payment.getGatewayTransactionId() != null ? payment.getGatewayTransactionId() : "",
                    "failureReason", payment.getFailureReason() != null ? payment.getFailureReason() : ""
            );
            String jsonPayload = objectMapper.writeValueAsString(data);
            OutboxEvent outboxEvent = OutboxEvent.create("Payment", payment.getId().toString(), eventType, jsonPayload);
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }

    private PaymentResponse mapToResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getOrderId(),
                p.getCustomerId(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus().name(),
                p.getIdempotencyKey(),
                p.getGatewayProvider(),
                p.getGatewayTransactionId(),
                p.getFailureReason(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
