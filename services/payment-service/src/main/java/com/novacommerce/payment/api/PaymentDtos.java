package com.novacommerce.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentDtos {

    public record AuthorizePaymentRequest(
            @NotNull UUID orderId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency,
            @NotBlank String idempotencyKey,
            @NotBlank String paymentToken
    ) {}

    public record CapturePaymentRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount
    ) {}

    public record RefundPaymentRequest(
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String reason
    ) {}

    public record PaymentResponse(
            UUID id,
            UUID orderId,
            UUID customerId,
            BigDecimal amount,
            String currency,
            String status,
            String idempotencyKey,
            String gatewayProvider,
            String gatewayTransactionId,
            String failureReason,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
