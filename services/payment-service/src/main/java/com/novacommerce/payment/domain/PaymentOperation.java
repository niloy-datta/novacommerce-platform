package com.novacommerce.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_operations")
public class PaymentOperation {
    @Id private UUID id;
    @Column(name = "payment_id", nullable = false) private UUID paymentId;
    @Enumerated(EnumType.STRING) @Column(name = "operation_type", nullable = false, length = 20) private PaymentOperationType type;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false, length = 20) private PaymentOperationStatus status;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200) private String idempotencyKey;
    @Column(name = "amount", precision = 19, scale = 2) private BigDecimal amount;
    @Column(name = "provider_operation_id", length = 128) private String providerOperationId;
    @Column(name = "failure_reason", columnDefinition = "TEXT") private String failureReason;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected PaymentOperation() { }
    private PaymentOperation(UUID id, UUID paymentId, PaymentOperationType type, String key, BigDecimal amount, Instant now) {
        this.id = id; this.paymentId = paymentId; this.type = type; this.idempotencyKey = key;
        this.amount = amount; this.status = PaymentOperationStatus.PENDING; this.createdAt = now; this.updatedAt = now;
    }
    public static PaymentOperation pending(UUID paymentId, PaymentOperationType type, String key, BigDecimal amount) {
        return new PaymentOperation(UUID.randomUUID(), paymentId, type, key, amount, Instant.now());
    }
    public void succeeded(String providerOperationId) { status = PaymentOperationStatus.SUCCEEDED; this.providerOperationId = providerOperationId; updatedAt = Instant.now(); }
    public void failed(String reason) { status = PaymentOperationStatus.FAILED; failureReason = reason; updatedAt = Instant.now(); }
    public void unknown(String reason) { status = PaymentOperationStatus.UNKNOWN; failureReason = reason; updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public PaymentOperationType getType() { return type; }
    public PaymentOperationStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public String getProviderOperationId() { return providerOperationId; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
