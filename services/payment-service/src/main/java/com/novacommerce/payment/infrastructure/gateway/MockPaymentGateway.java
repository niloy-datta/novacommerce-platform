package com.novacommerce.payment.infrastructure.gateway;

import com.novacommerce.payment.domain.PaymentGateway;
import com.novacommerce.payment.domain.PaymentGatewayResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component("mockPaymentGateway")
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    @Override
    public PaymentGatewayResult authorize(UUID orderId, BigDecimal amount, String currency, String paymentToken) {
        if ("fail_token".equalsIgnoreCase(paymentToken)) {
            return PaymentGatewayResult.failure("Payment authorization declined by mock gateway");
        }
        String mockTxId = "mock_auth_" + UUID.randomUUID().toString().substring(0, 8);
        return PaymentGatewayResult.success(mockTxId, "AUTHORIZED");
    }

    @Override
    public PaymentGatewayResult capture(String gatewayTransactionId, BigDecimal amount) {
        if (gatewayTransactionId == null || gatewayTransactionId.contains("fail")) {
            return PaymentGatewayResult.failure("Capture failed for transaction " + gatewayTransactionId);
        }
        String mockTxId = "mock_cap_" + UUID.randomUUID().toString().substring(0, 8);
        return PaymentGatewayResult.success(mockTxId, "CAPTURED");
    }

    @Override
    public PaymentGatewayResult refund(String gatewayTransactionId, BigDecimal amount) {
        if (gatewayTransactionId == null || gatewayTransactionId.contains("fail")) {
            return PaymentGatewayResult.failure("Refund failed for transaction " + gatewayTransactionId);
        }
        String mockTxId = "mock_ref_" + UUID.randomUUID().toString().substring(0, 8);
        return PaymentGatewayResult.success(mockTxId, "REFUNDED");
    }
}
