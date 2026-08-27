package com.novacommerce.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {
    @Id private UUID id;
    @Column(name = "order_id", nullable = false) private UUID orderId;
    @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "currency", nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 32) private PaymentStatus status;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128) private String idempotencyKey;
    @Column(name = "gateway_provider", nullable = false, length = 32) private String gatewayProvider;
    @Column(name = "gateway_transaction_id", length = 128) private String gatewayTransactionId;
    @Column(name = "failure_reason", columnDefinition = "TEXT") private String failureReason;
    @Column(name = "authorized_amount", nullable = false, precision = 19, scale = 2) private BigDecimal authorizedAmount;
    @Column(name = "captured_amount", nullable = false, precision = 19, scale = 2) private BigDecimal capturedAmount;
    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2) private BigDecimal refundedAmount;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected Payment() { }

    public Payment(UUID id, UUID orderId, UUID customerId, BigDecimal amount, String currency,
                   PaymentStatus status, String idempotencyKey, String gatewayProvider,
                   String gatewayTransactionId, String failureReason, Instant createdAt, Instant updatedAt) {
        this(id, orderId, customerId, amount, currency, status, idempotencyKey, gatewayProvider,
                gatewayTransactionId, failureReason, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, createdAt, updatedAt);
    }

    public Payment(UUID id, UUID orderId, UUID customerId, BigDecimal amount, String currency,
                   PaymentStatus status, String idempotencyKey, String gatewayProvider,
                   String gatewayTransactionId, String failureReason, BigDecimal authorizedAmount,
                   BigDecimal capturedAmount, BigDecimal refundedAmount, Instant createdAt, Instant updatedAt) {
        if (id == null || orderId == null || customerId == null || amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment identity and positive amount are required");
        }
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount.setScale(2);
        this.currency = normalizeCurrency(currency);
        this.status = Objects.requireNonNull(status);
        this.idempotencyKey = requireText(idempotencyKey, "Idempotency key");
        this.gatewayProvider = requireText(gatewayProvider, "Gateway provider");
        this.gatewayTransactionId = gatewayTransactionId;
        this.failureReason = failureReason;
        this.authorizedAmount = moneyOrZero(authorizedAmount);
        this.capturedAmount = moneyOrZero(capturedAmount);
        this.refundedAmount = moneyOrZero(refundedAmount);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Payment createPending(UUID id, UUID orderId, UUID customerId, BigDecimal amount,
                                        String currency, String idempotencyKey, String gatewayProvider) {
        Instant now = Instant.now();
        return new Payment(id, orderId, customerId, amount, currency, PaymentStatus.PENDING,
                idempotencyKey, gatewayProvider, null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, now, now);
    }

    public void authorize(String providerPaymentId) { authorize(providerPaymentId, amount); }
    public void authorize(String providerPaymentId, BigDecimal authorizedAmount) {
        requireStatus(PaymentStatus.PENDING);
        if (authorizedAmount == null || authorizedAmount.signum() <= 0 || authorizedAmount.compareTo(amount) > 0) {
            throw new IllegalArgumentException("Authorized amount must be positive and no greater than payment amount");
        }
        gatewayTransactionId = requireText(providerPaymentId, "Provider payment id");
        this.authorizedAmount = authorizedAmount.setScale(2);
        status = PaymentStatus.AUTHORIZED;
        touch();
    }

    public void capture() { capture(amount); }
    public void capture(BigDecimal captureAmount) {
        requireStatus(PaymentStatus.AUTHORIZED);
        BigDecimal requested = money(captureAmount);
        if (requested.compareTo(authorizedAmount.subtract(capturedAmount)) > 0) {
            throw new IllegalArgumentException("Capture amount exceeds authorized amount");
        }
        capturedAmount = capturedAmount.add(requested).setScale(2);
        status = capturedAmount.compareTo(amount) >= 0 ? PaymentStatus.CAPTURED : PaymentStatus.AUTHORIZED;
        touch();
    }

    public void fail(String reason) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment cannot fail from " + status);
        }
        status = PaymentStatus.FAILED;
        failureReason = requireText(reason, "Failure reason");
        touch();
    }

    public void cancel(String reason) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment cannot be cancelled from " + status);
        }
        status = PaymentStatus.CANCELLED;
        failureReason = reason;
        touch();
    }

    public void refund(BigDecimal refundAmount) {
        if (status != PaymentStatus.CAPTURED && status != PaymentStatus.AUTHORIZED
                && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Payment cannot be refunded from " + status);
        }
        BigDecimal requested = money(refundAmount);
        BigDecimal refundable = capturedAmount.subtract(refundedAmount);
        if (requested.compareTo(refundable) > 0) throw new IllegalArgumentException("Refund exceeds refundable amount");
        refundedAmount = refundedAmount.add(requested).setScale(2);
        status = refundedAmount.compareTo(capturedAmount) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED;
        touch();
    }

    public void refund() { refund(capturedAmount.signum() > 0 ? capturedAmount : amount); }
    private void requireStatus(PaymentStatus expected) { if (status != expected) throw new IllegalStateException("Payment must be " + expected + ", but was " + status); }
    private void touch() { updatedAt = Instant.now(); }
    private static BigDecimal money(BigDecimal value) { if (value == null || value.signum() <= 0) throw new IllegalArgumentException("Amount must be positive"); return value.setScale(2); }
    private static BigDecimal moneyOrZero(BigDecimal value) { return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2); }
    private static String normalizeCurrency(String value) { return requireText(value, "Currency").toUpperCase(java.util.Locale.ROOT); }
    private static String requireText(String value, String label) { if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required"); return value; }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getGatewayProvider() { return gatewayProvider; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public String getProviderPaymentId() { return gatewayTransactionId; }
    public String getFailureReason() { return failureReason; }
    public BigDecimal getAuthorizedAmount() { return authorizedAmount; }
    public BigDecimal getCapturedAmount() { return capturedAmount; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    @Override public boolean equals(Object o) { return this == o || (o instanceof Payment p && Objects.equals(id, p.id)); }
    @Override public int hashCode() { return Objects.hash(id); }
}
