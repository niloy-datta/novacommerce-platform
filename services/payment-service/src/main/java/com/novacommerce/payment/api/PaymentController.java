package com.novacommerce.payment.api;

import com.novacommerce.payment.api.PaymentDtos.*;
import com.novacommerce.payment.application.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/authorize")
    public ResponseEntity<PaymentResponse> authorizePayment(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody AuthorizePaymentRequest request) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        PaymentResponse response = paymentService.authorizePayment(customerId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(@PathVariable UUID paymentId,
                                                           @Valid @RequestBody CapturePaymentRequest request) {
        PaymentResponse response = paymentService.capturePayment(paymentId, request.amount());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable UUID paymentId,
                                                          @Valid @RequestBody RefundPaymentRequest request) {
        PaymentResponse response = paymentService.refundPayment(paymentId, request.amount(), request.reason());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsForOrder(@PathVariable UUID orderId) {
        List<PaymentResponse> responses = paymentService.getPaymentsForOrder(orderId);
        return ResponseEntity.ok(responses);
    }
}
