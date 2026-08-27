package com.novacommerce.payment.domain;

public record PaymentGatewayResult(
        boolean success,
        String providerPaymentId,
        String operationId,
        String status,
        String errorMessage,
        boolean outcomeUnknown
) {
    public static PaymentGatewayResult success(String gatewayTransactionId, String status) {
        return new PaymentGatewayResult(true, gatewayTransactionId, gatewayTransactionId, status, null, false);
    }

    public static PaymentGatewayResult failure(String errorMessage) {
        return new PaymentGatewayResult(false, null, null, "FAILED", errorMessage, false);
    }

    public static PaymentGatewayResult unknown(String message) {
        return new PaymentGatewayResult(false, null, null, "PENDING", message, true);
    }
}
