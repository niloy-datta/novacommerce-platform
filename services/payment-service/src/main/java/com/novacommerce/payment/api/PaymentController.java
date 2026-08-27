package com.novacommerce.payment.api;

import com.novacommerce.payment.api.PaymentDtos.*;
import com.novacommerce.payment.application.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService payments;
    public PaymentController(PaymentService payments) { this.payments = payments; }

    @PostMapping({"", "/authorize"})
    public ResponseEntity<PaymentResponse> authorize(@AuthenticationPrincipal Jwt jwt,
                                                       @RequestHeader(value = "Idempotency-Key", required = false) String headerKey,
                                                       @Valid @RequestBody AuthorizePaymentRequest request) {
        if (headerKey != null && !headerKey.equals(request.idempotencyKey())) {
            throw new com.novacommerce.payment.api.error.PaymentException(org.springframework.http.HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_MISMATCH", "Body and header idempotency keys must match");
        }
        return ResponseEntity.ok(payments.authorizePayment(subject(jwt), request));
    }

    @PostMapping("/{paymentId}/capture")
    public PaymentResponse capture(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
                                   @RequestHeader("Idempotency-Key") String key,
                                   @Valid @RequestBody CapturePaymentRequest request) {
        return payments.capturePayment(subject(jwt), paymentId, request.amount(), key, admin(jwt));
    }

    @PostMapping("/{paymentId}/cancel")
    public PaymentResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
                                  @RequestHeader("Idempotency-Key") String key,
                                  @RequestBody(required = false) CancelPaymentRequest request) {
        return payments.cancelPayment(subject(jwt), paymentId, key, request == null ? null : request.reason(), admin(jwt));
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentResponse refund(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
                                  @RequestHeader("Idempotency-Key") String key,
                                  @Valid @RequestBody RefundPaymentRequest request) {
        return payments.refundPayment(subject(jwt), paymentId, request.amount(), key, admin(jwt));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return payments.getPayment(subject(jwt), paymentId, admin(jwt));
    }

    @GetMapping("/order/{orderId}")
    public List<PaymentResponse> getForOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID orderId) {
        return payments.getPaymentsForOrder(subject(jwt), orderId, admin(jwt));
    }

    private static UUID subject(Jwt jwt) { try { return UUID.fromString(jwt.getSubject()); } catch (Exception ex) { throw new com.novacommerce.payment.api.error.PaymentException(org.springframework.http.HttpStatus.UNAUTHORIZED, "INVALID_SUBJECT", "JWT subject must be a UUID"); } }
    private static boolean admin(Jwt jwt) { List<String> roles = jwt.getClaimAsStringList("roles"); return roles != null && roles.contains("ADMIN"); }
}
