package com.novacommerce.order.domain.order;

public enum OrderStatus {
    PENDING_INVENTORY,
    AWAITING_PAYMENT,
    CONFIRMED,
    PAYMENT_FAILED,
    CANCELLED,
    EXPIRED
}
