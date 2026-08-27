package com.novacommerce.payment.infrastructure.gateway;

import com.novacommerce.payment.config.PaymentProperties;
import com.novacommerce.payment.domain.PaymentGateway;
import com.novacommerce.payment.domain.PaymentGatewayResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component("stripePaymentGateway")
public class StripePaymentGateway implements PaymentGateway {

    private final PaymentProperties properties;

    public StripePaymentGateway(PaymentProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getProviderName() {
        return "STRIPE";
    }

    @Override
    public PaymentGatewayResult authorize(UUID orderId, BigDecimal amount, String currency, String paymentToken, String idempotencyKey) {
        // Stripe Sandbox Simulation / REST API Adapter
        if ("tok_chargeCustomerFail".equalsIgnoreCase(paymentToken)) {
            return PaymentGatewayResult.failure("Your card was declined by Stripe Sandbox.");
        }
        String stripeTxId = "ch_stripe_" + UUID.randomUUID().toString().substring(0, 12);
        return PaymentGatewayResult.success(stripeTxId, "AUTHORIZED");
    }

    @Override
    public PaymentGatewayResult capture(String gatewayTransactionId, BigDecimal amount, String idempotencyKey) {
        String stripeTxId = "cap_stripe_" + UUID.randomUUID().toString().substring(0, 12);
        return PaymentGatewayResult.success(stripeTxId, "CAPTURED");
    }

    @Override
    public PaymentGatewayResult cancel(String gatewayTransactionId, String idempotencyKey) {
        return PaymentGatewayResult.success(gatewayTransactionId, "CANCELLED");
    }

    @Override
    public PaymentGatewayResult refund(String gatewayTransactionId, BigDecimal amount, String idempotencyKey) {
        String stripeTxId = "re_stripe_" + UUID.randomUUID().toString().substring(0, 12);
        return PaymentGatewayResult.success(stripeTxId, "REFUNDED");
    }
}
