package com.novacommerce.order.domain.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "orders")
public class CustomerOrder {

    @Id
    private UUID id;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "source_cart_id")
    private UUID sourceCartId;

    @Column(name = "checkout_idempotency_key")
    private String idempotencyKey;

    @Column(name = "checkout_request_hash")
    private String requestHash;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String currency;

    @Column(name = "merchandise_total")
    private BigDecimal merchandiseTotal;

    @Column(name = "inventory_reservation_id")
    private UUID reservationId;

    @Column(name = "inventory_reservation_expires_at")
    private Instant reservationExpiresAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Version
    private long version;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    protected CustomerOrder() {
    }

    public CustomerOrder(UUID owner, UUID cart, String key, String hash, String currency, Instant now) {
        id = UUID.randomUUID();
        ownerId = owner;
        sourceCartId = cart;
        idempotencyKey = key;
        requestHash = hash;
        this.currency = currency;
        status = OrderStatus.PENDING_INVENTORY;
        merchandiseTotal = BigDecimal.ZERO.setScale(2);
        createdAt = updatedAt = now;
    }

    public void addItem(UUID variant, UUID product, String productName, String slug, String variantName, String sku, Map<String, String> attrs, long q, BigDecimal price) {
        OrderItem item = new OrderItem(this, variant, product, productName, slug, variantName, sku, attrs, q, price, currency);
        items.add(item);
        merchandiseTotal = merchandiseTotal.add(item.getLineTotal());
    }

    public void inventoryReserved(UUID id, Instant expiry, Instant now) {
        if (status == OrderStatus.PENDING_INVENTORY) {
            reservationId = id;
            reservationExpiresAt = expiry;
            status = OrderStatus.AWAITING_PAYMENT;
            updatedAt = now;
        }
    }

    public void confirmPayment(Instant now) {
        if (status == OrderStatus.AWAITING_PAYMENT) {
            status = OrderStatus.CONFIRMED;
            updatedAt = now;
        }
    }

    public void paymentFailed(String reason, Instant now) {
        if (status == OrderStatus.AWAITING_PAYMENT || status == OrderStatus.PENDING_INVENTORY) {
            status = OrderStatus.PAYMENT_FAILED;
            cancellationReason = reason;
            updatedAt = now;
        }
    }

    public void cancel(String reason, Instant now) {
        if (status == OrderStatus.PENDING_INVENTORY || status == OrderStatus.AWAITING_PAYMENT) {
            status = OrderStatus.CANCELLED;
            cancellationReason = reason;
            updatedAt = now;
        }
    }

    public void expire(Instant now) {
        if (status == OrderStatus.AWAITING_PAYMENT || status == OrderStatus.PENDING_INVENTORY) {
            status = OrderStatus.EXPIRED;
            updatedAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getSourceCartId() {
        return sourceCartId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getMerchandiseTotal() {
        return merchandiseTotal;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public Instant getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}
