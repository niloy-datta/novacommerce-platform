package com.novacommerce.payment.domain;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    String getProviderName();

    PaymentGatewayResult authorize(UUID orderId, BigDecimal amount, String currency, String paymentToken);

    PaymentGatewayResult capture(String gatewayTransactionId, BigDecimal amount);

    PaymentGatewayResult refund(String gatewayTransactionId, BigDecimal amount);
}
