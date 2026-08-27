package com.novacommerce.payment.domain;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    String getProviderName();

    PaymentGatewayResult authorize(UUID orderId, BigDecimal amount, String currency, String paymentToken, String idempotencyKey);

    PaymentGatewayResult capture(String providerPaymentId, BigDecimal amount, String idempotencyKey);

    PaymentGatewayResult cancel(String providerPaymentId, String idempotencyKey);

    PaymentGatewayResult refund(String providerPaymentId, BigDecimal amount, String idempotencyKey);

    default PaymentGatewayResult retrieve(String providerPaymentId) {
        return PaymentGatewayResult.failure("Provider retrieval is not supported");
    }
}
