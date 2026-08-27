package com.novacommerce.payment.application;

import com.novacommerce.payment.api.PaymentDtos;
import com.novacommerce.payment.api.error.PaymentException;
import com.novacommerce.payment.domain.*;
import com.novacommerce.payment.infrastructure.outbox.OutboxEvent;
import com.novacommerce.payment.infrastructure.outbox.OutboxRepository;
import com.novacommerce.payment.infrastructure.persistence.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentOperationCoordinator {
    private final PaymentRepository payments;
    private final PaymentOperationRepository operations;
    private final OutboxRepository outbox;
    private final ProcessedWebhookEventRepository webhookEvents;
    private final PaymentIdempotencyLock idempotencyLock;
    private final ObjectMapper mapper;

    public PaymentOperationCoordinator(PaymentRepository payments, PaymentOperationRepository operations,
                                       OutboxRepository outbox, ProcessedWebhookEventRepository webhookEvents,
                                       PaymentIdempotencyLock idempotencyLock, ObjectMapper mapper) {
        this.payments = payments; this.operations = operations; this.outbox = outbox;
        this.webhookEvents = webhookEvents; this.idempotencyLock = idempotencyLock; this.mapper = mapper;
    }

    @Transactional
    public Preparation prepareAuthorization(UUID customerId, PaymentDtos.AuthorizePaymentRequest request, String provider) {
        validateKey(request.idempotencyKey());
        idempotencyLock.acquire(request.idempotencyKey());
        Payment existing = payments.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (!existing.getCustomerId().equals(customerId) || !existing.getOrderId().equals(request.orderId())
                    || existing.getAmount().compareTo(request.amount().setScale(2)) != 0
                    || !existing.getCurrency().equalsIgnoreCase(request.currency())) {
                throw error(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "Idempotency key was used for another payment");
            }
            PaymentOperation operation = operations.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
            return new Preparation(existing, operation, false);
        }
        boolean alreadyPaid = payments.findByOrderId(request.orderId()).stream().anyMatch(p ->
                p.getStatus() == PaymentStatus.AUTHORIZED || p.getStatus() == PaymentStatus.CAPTURED
                        || p.getStatus() == PaymentStatus.PARTIALLY_REFUNDED || p.getStatus() == PaymentStatus.REFUNDED);
        if (alreadyPaid) throw error(HttpStatus.CONFLICT, "ORDER_ALREADY_PAID", "Order already has an authorized or captured payment");
        Payment payment = payments.save(Payment.createPending(UUID.randomUUID(), request.orderId(), customerId,
                request.amount(), request.currency(), request.idempotencyKey(), provider));
        PaymentOperation operation = operations.save(PaymentOperation.pending(payment.getId(), PaymentOperationType.AUTHORIZE,
                request.idempotencyKey(), request.amount()));
        return new Preparation(payment, operation, true);
    }

    @Transactional
    public Payment applyAuthorization(String key, PaymentGatewayResult result) {
        PaymentOperation operation = lockOperation(key);
        Payment payment = lockPayment(operation.getPaymentId());
        if (operation.getStatus() != PaymentOperationStatus.PENDING) return payment;
        if (result.outcomeUnknown()) { operation.unknown(result.errorMessage()); return payment; }
        if (result.success()) {
            payment.authorize(result.providerPaymentId(), payment.getAmount());
            operation.succeeded(result.operationId());
            saveEvent("PaymentAuthorized", payment);
        } else {
            payment.fail(result.errorMessage());
            operation.failed(result.errorMessage());
            saveEvent("PaymentFailed", payment);
        }
        return payments.save(payment);
    }

    @Transactional
    public OperationPreparation prepareOperation(UUID customerId, UUID paymentId, PaymentOperationType type,
                                                 BigDecimal amount, String key, boolean admin) {
        validateKey(key);
        idempotencyLock.acquire(key);
        Payment payment = lockPayment(paymentId);
        if (!admin && !payment.getCustomerId().equals(customerId)) throw error(HttpStatus.FORBIDDEN, "PAYMENT_ACCESS_DENIED", "Payment belongs to another customer");
        PaymentOperation existing = operations.findByIdempotencyKey(key).orElse(null);
        if (existing != null) {
            if (!existing.getPaymentId().equals(paymentId) || existing.getType() != type) throw error(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "Idempotency key was used for another operation");
            return new OperationPreparation(payment, existing, false);
        }
        if ((type == PaymentOperationType.CAPTURE || type == PaymentOperationType.CANCEL)
                && operations.findByPaymentIdAndType(paymentId, type).isPresent()) {
            throw error(HttpStatus.CONFLICT, "OPERATION_ALREADY_EXISTS", "This payment operation has already been requested");
        }
        validateOperationState(payment, type, amount);
        return new OperationPreparation(payment, operations.save(PaymentOperation.pending(paymentId, type, key, amount)), true);
    }

    @Transactional
    public Payment applyOperation(String key, PaymentGatewayResult result) {
        PaymentOperation operation = lockOperation(key);
        Payment payment = lockPayment(operation.getPaymentId());
        if (operation.getStatus() != PaymentOperationStatus.PENDING) return payment;
        if (result.outcomeUnknown()) { operation.unknown(result.errorMessage()); return payment; }
        if (result.success()) {
            switch (operation.getType()) {
                case CAPTURE -> payment.capture(operation.getAmount());
                case CANCEL -> payment.cancel(null);
                case REFUND -> payment.refund(operation.getAmount());
                default -> throw error(HttpStatus.CONFLICT, "INVALID_OPERATION", "Unsupported payment operation");
            }
            operation.succeeded(result.operationId());
            saveEvent(eventFor(operation.getType()), payment);
        } else {
            operation.failed(result.errorMessage());
        }
        return payments.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment get(UUID customerId, UUID id, boolean admin) {
        Payment payment = payments.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));
        if (!admin && !payment.getCustomerId().equals(customerId)) throw error(HttpStatus.FORBIDDEN, "PAYMENT_ACCESS_DENIED", "Payment belongs to another customer");
        return payment;
    }

    @Transactional(readOnly = true)
    public java.util.List<Payment> getForOrder(UUID customerId, UUID orderId, boolean admin) {
        java.util.List<Payment> result = payments.findByOrderId(orderId);
        if (!admin && result.stream().anyMatch(p -> !p.getCustomerId().equals(customerId))) throw error(HttpStatus.FORBIDDEN, "PAYMENT_ACCESS_DENIED", "Payment belongs to another customer");
        return result;
    }

    @Transactional
    public boolean acceptWebhook(String eventId, String eventType) {
        if (webhookEvents.existsById(eventId)) return false;
        webhookEvents.save(new ProcessedWebhookEvent(eventId, eventType, java.time.Instant.now()));
        return true;
    }

    @Transactional
    public Payment applyWebhook(String providerPaymentId, String eventType, BigDecimal amount, String reason) {
        Payment payment = payments.findByGatewayTransactionId(providerPaymentId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment for webhook was not found"));
        if ("payment_intent.succeeded".equals(eventType)) {
            if (payment.getStatus() == PaymentStatus.PENDING) payment.authorize(providerPaymentId, amount);
            if (payment.getStatus() == PaymentStatus.AUTHORIZED) payment.capture(amount);
            saveEvent("PaymentCaptured", payment);
        } else if ("payment_intent.amount_capturable_updated".equals(eventType) && payment.getStatus() == PaymentStatus.PENDING) {
            payment.authorize(providerPaymentId, amount); saveEvent("PaymentAuthorized", payment);
        } else if ("payment_intent.payment_failed".equals(eventType) && payment.getStatus() == PaymentStatus.PENDING) {
            payment.fail(reason == null ? "Provider reported payment failure" : reason); saveEvent("PaymentFailed", payment);
        }
        return payments.save(payment);
    }

    private PaymentOperation lockOperation(String key) { return operations.lockByIdempotencyKey(key).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "OPERATION_NOT_FOUND", "Payment operation not found")); }
    private Payment lockPayment(UUID id) { return payments.lockById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found")); }
    private static void validateKey(String key) { if (key == null || key.isBlank() || key.length() > 200) throw error(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", "A non-empty Idempotency-Key is required"); }
    private static void validateOperationState(Payment p, PaymentOperationType type, BigDecimal amount) {
        if (type == PaymentOperationType.CAPTURE && (p.getStatus() != PaymentStatus.AUTHORIZED || amount == null || amount.signum() <= 0)) throw error(HttpStatus.CONFLICT, "PAYMENT_NOT_CAPTUREABLE", "Payment is not authorized for this capture");
        if (type == PaymentOperationType.CANCEL && p.getStatus() != PaymentStatus.AUTHORIZED) throw error(HttpStatus.CONFLICT, "PAYMENT_NOT_CANCELLABLE", "Payment is not authorized");
        if (type == PaymentOperationType.REFUND && (p.getStatus() != PaymentStatus.CAPTURED && p.getStatus() != PaymentStatus.PARTIALLY_REFUNDED)) throw error(HttpStatus.CONFLICT, "PAYMENT_NOT_REFUNDABLE", "Payment has no refundable capture");
    }
    private void saveEvent(String type, Payment payment) {
        try {
            String payload = mapper.writeValueAsString(Map.of("paymentId", payment.getId(), "orderId", payment.getOrderId(), "customerId", payment.getCustomerId(), "amount", payment.getAmount(), "currency", payment.getCurrency(), "status", payment.getStatus().name(), "capturedAmount", payment.getCapturedAmount(), "refundedAmount", payment.getRefundedAmount()));
            outbox.save(OutboxEvent.create("Payment", payment.getId().toString(), type, payload));
        } catch (JacksonException ex) { throw new IllegalStateException("Unable to serialize payment event", ex); }
    }
    private static String eventFor(PaymentOperationType type) { return switch (type) { case CAPTURE -> "PaymentCaptured"; case CANCEL -> "PaymentCancelled"; case REFUND -> "RefundCompleted"; case AUTHORIZE -> "PaymentAuthorized"; }; }
    private static PaymentException error(HttpStatus status, String code, String message) { return new PaymentException(status, code, message); }
    public record Preparation(Payment payment, PaymentOperation operation, boolean executeGateway) { }
    public record OperationPreparation(Payment payment, PaymentOperation operation, boolean executeGateway) { }
}
