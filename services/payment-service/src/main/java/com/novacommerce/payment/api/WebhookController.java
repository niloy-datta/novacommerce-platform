package com.novacommerce.payment.api;

import com.novacommerce.payment.application.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class WebhookController {

    private final PaymentService paymentService;

    public WebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Map<String, String>> handleStripeWebhook(@RequestBody String payload,
                                                                   @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        paymentService.processStripeWebhook(payload, signature);
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
