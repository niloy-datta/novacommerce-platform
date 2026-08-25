package com.novacommerce.payment.domain;

public record PaymentGatewayResult(
        boolean success,
        String gatewayTransactionId,
        String status,
        String errorMessage
) {
    public static PaymentGatewayResult success(String gatewayTransactionId, String status) {
        return new PaymentGatewayResult(true, gatewayTransactionId, status, null);
    }

    public static PaymentGatewayResult failure(String errorMessage) {
        return new PaymentGatewayResult(false, null, "FAILED", errorMessage);
    }
}
