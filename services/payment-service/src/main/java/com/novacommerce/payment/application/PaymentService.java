package com.novacommerce.payment.application;

import com.novacommerce.payment.api.PaymentDtos;
import com.novacommerce.payment.api.error.PaymentException;
import com.novacommerce.payment.config.PaymentProperties;
import com.novacommerce.payment.domain.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentService {
    private final PaymentOperationCoordinator coordinator;
    private final Map<String, PaymentGateway> gateways;
    private final PaymentProperties properties;
    private final ObjectMapper mapper;

    public PaymentService(PaymentOperationCoordinator coordinator, List<PaymentGateway> gatewayList,
                          PaymentProperties properties, ObjectMapper mapper) {
        this.coordinator = coordinator; this.properties = properties; this.mapper = mapper;
        gateways = new HashMap<>();
        gatewayList.forEach(g -> gateways.put(g.getProviderName().toUpperCase(Locale.ROOT), g));
    }

    public PaymentDtos.PaymentResponse authorizePayment(UUID customerId, PaymentDtos.AuthorizePaymentRequest request) {
        String providerName = properties.getProvider().toUpperCase(Locale.ROOT);
        PaymentGateway gateway = gateway(providerName);
        var prepared = coordinator.prepareAuthorization(customerId, request, gateway.getProviderName());
        if (!prepared.executeGateway()) return response(prepared.payment());
        PaymentGatewayResult result;
        try {
            result = gateway.authorize(request.orderId(), request.amount(), request.currency(), request.paymentToken(), request.idempotencyKey());
        } catch (RuntimeException ex) {
            result = PaymentGatewayResult.unknown("Provider outcome is unknown; reconcile using the payment reference");
        }
        return response(coordinator.applyAuthorization(request.idempotencyKey(), result));
    }

    public PaymentDtos.PaymentResponse capturePayment(UUID customerId, UUID paymentId, BigDecimal amount, String key, boolean admin) {
        var prepared = coordinator.prepareOperation(customerId, paymentId, PaymentOperationType.CAPTURE, amount, key, admin);
        if (!prepared.executeGateway()) return response(prepared.payment());
        PaymentGatewayResult result;
        try { result = gateway(prepared.payment().getGatewayProvider()).capture(prepared.payment().getProviderPaymentId(), amount, key); }
        catch (RuntimeException ex) { result = PaymentGatewayResult.unknown("Capture outcome is unknown; reconcile using the operation key"); }
        return response(coordinator.applyOperation(key, result));
    }

    public PaymentDtos.PaymentResponse cancelPayment(UUID customerId, UUID paymentId, String key, String reason, boolean admin) {
        var prepared = coordinator.prepareOperation(customerId, paymentId, PaymentOperationType.CANCEL, null, key, admin);
        if (!prepared.executeGateway()) return response(prepared.payment());
        PaymentGatewayResult result;
        try { result = gateway(prepared.payment().getGatewayProvider()).cancel(prepared.payment().getProviderPaymentId(), key); }
        catch (RuntimeException ex) { result = PaymentGatewayResult.unknown("Cancel outcome is unknown; reconcile using the operation key"); }
        return response(coordinator.applyOperation(key, result));
    }

    public PaymentDtos.PaymentResponse refundPayment(UUID customerId, UUID paymentId, BigDecimal amount, String key, boolean admin) {
        var prepared = coordinator.prepareOperation(customerId, paymentId, PaymentOperationType.REFUND, amount, key, admin);
        if (!prepared.executeGateway()) return response(prepared.payment());
        PaymentGatewayResult result;
        try { result = gateway(prepared.payment().getGatewayProvider()).refund(prepared.payment().getProviderPaymentId(), amount, key); }
        catch (RuntimeException ex) { result = PaymentGatewayResult.unknown("Refund outcome is unknown; reconcile using the operation key"); }
        return response(coordinator.applyOperation(key, result));
    }

    public PaymentDtos.PaymentResponse getPayment(UUID customerId, UUID id, boolean admin) { return response(coordinator.get(customerId, id, admin)); }
    public List<PaymentDtos.PaymentResponse> getPaymentsForOrder(UUID customerId, UUID orderId, boolean admin) { return coordinator.getForOrder(customerId, orderId, admin).stream().map(PaymentService::response).toList(); }

    public void processStripeWebhook(String payload, String signature) {
        verifyStripeSignature(payload, signature);
        try {
            JsonNode root = mapper.readTree(payload);
            String eventId = text(root, "id");
            String type = text(root, "type");
            JsonNode object = root.path("data").path("object");
            String providerId = text(object, "id");
            if (eventId.isBlank() || type.isBlank() || providerId.isBlank()) throw error(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK", "Webhook payload is incomplete");
            if (!coordinator.acceptWebhook(eventId, type)) return;
            BigDecimal amount = BigDecimal.valueOf(object.path("amount_received").asLong(object.path("amount").asLong())).movePointLeft(2);
            coordinator.applyWebhook(providerId, type, amount.signum() > 0 ? amount : null, text(object, "last_payment_error"));
        } catch (PaymentException ex) { throw ex; }
        catch (Exception ex) { throw error(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK", "Webhook payload is invalid"); }
    }

    private void verifyStripeSignature(String payload, String signature) {
        String secret = properties.getStripe().getWebhookSecret();
        if (secret == null || secret.isBlank() || signature == null || signature.isBlank()) throw error(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Invalid webhook signature");
        String timestamp = null, expected = null;
        for (String part : signature.split(",")) {
            String[] pieces = part.split("=", 2);
            if (pieces.length != 2) continue;
            if (pieces[0].equals("t")) timestamp = pieces[1];
            if (pieces[0].equals("v1")) expected = pieces[1];
        }
        try {
            long signedAt = Long.parseLong(timestamp == null ? "-1" : timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - signedAt) > properties.getWebhookToleranceSeconds()) throw error(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Webhook timestamp is outside tolerance");
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String actual = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
            if (expected == null || !MessageDigest.isEqual(actual.getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII))) throw error(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Invalid webhook signature");
        } catch (PaymentException ex) { throw ex; }
        catch (Exception ex) { throw error(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Invalid webhook signature"); }
    }

    private PaymentGateway gateway(String name) { PaymentGateway gateway = gateways.get(name.toUpperCase(Locale.ROOT)); if (gateway == null) throw error(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_PROVIDER_UNAVAILABLE", "Configured payment provider is unavailable"); return gateway; }
    private static String text(JsonNode node, String field) { JsonNode value = node.path(field); return value.isTextual() ? value.asString() : ""; }
    private static PaymentDtos.PaymentResponse response(Payment p) { return new PaymentDtos.PaymentResponse(p.getId(), p.getOrderId(), p.getCustomerId(), p.getAmount(), p.getCurrency(), p.getStatus().name(), p.getIdempotencyKey(), p.getGatewayProvider(), p.getProviderPaymentId(), p.getFailureReason(), p.getAuthorizedAmount(), p.getCapturedAmount(), p.getRefundedAmount(), p.getCreatedAt(), p.getUpdatedAt()); }
    private static PaymentException error(HttpStatus status, String code, String message) { return new PaymentException(status, code, message); }
}
