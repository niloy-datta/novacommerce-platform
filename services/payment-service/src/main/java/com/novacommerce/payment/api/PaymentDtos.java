package com.novacommerce.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {
    private PaymentDtos() { }

    public record AuthorizePaymentRequest(
            @NotNull UUID orderId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotBlank String idempotencyKey,
            @NotBlank String paymentToken
    ) { }

    public record CapturePaymentRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) { }
    public record CancelPaymentRequest(String reason) { }
    public record RefundPaymentRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, String reason) { }

    public record PaymentResponse(
            UUID id, UUID orderId, UUID customerId, BigDecimal amount, String currency, String status,
            String idempotencyKey, String gatewayProvider, String gatewayTransactionId, String failureReason,
            BigDecimal authorizedAmount, BigDecimal capturedAmount, BigDecimal refundedAmount,
            Instant createdAt, Instant updatedAt
    ) { }
}
