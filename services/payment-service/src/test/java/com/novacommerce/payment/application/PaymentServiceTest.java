package com.novacommerce.payment.application;

import com.novacommerce.payment.api.PaymentDtos;
import com.novacommerce.payment.config.PaymentProperties;
import com.novacommerce.payment.domain.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    @Test
    void authorizationUsesGatewayOnceAndReturnsAuthorizedAmounts() {
        PaymentOperationCoordinator coordinator = mock(PaymentOperationCoordinator.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        when(gateway.getProviderName()).thenReturn("MOCK");
        PaymentProperties properties = new PaymentProperties(); properties.setProvider("MOCK");
        PaymentService service = new PaymentService(coordinator, List.of(gateway), properties, new ObjectMapper());
        UUID customer = UUID.randomUUID(), order = UUID.randomUUID();
        String key = "authorize-1";
        Payment payment = Payment.createPending(UUID.randomUUID(), order, customer, new BigDecimal("10.00"), "USD", key, "MOCK");
        PaymentOperation operation = PaymentOperation.pending(payment.getId(), PaymentOperationType.AUTHORIZE, key, payment.getAmount());
        when(coordinator.prepareAuthorization(eq(customer), any(), eq("MOCK"))).thenReturn(new PaymentOperationCoordinator.Preparation(payment, operation, true));
        when(gateway.authorize(order, payment.getAmount(), "USD", "tok", key)).thenReturn(PaymentGatewayResult.success("mock-payment", "AUTHORIZED"));
        Payment authorized = Payment.createPending(payment.getId(), order, customer, payment.getAmount(), "USD", key, "MOCK");
        authorized.authorize("mock-payment");
        when(coordinator.applyAuthorization(eq(key), any())).thenReturn(authorized);

        PaymentDtos.PaymentResponse response = service.authorizePayment(customer,
                new PaymentDtos.AuthorizePaymentRequest(order, new BigDecimal("10.00"), "USD", key, "tok"));

        assertThat(response.status()).isEqualTo("AUTHORIZED");
        assertThat(response.authorizedAmount()).isEqualByComparingTo("10.00");
        verify(gateway).authorize(order, payment.getAmount(), "USD", "tok", key);
    }

    @Test
    void repeatedAuthorizationWithSameKeyDoesNotCallGatewayAgain() {
        PaymentOperationCoordinator coordinator = mock(PaymentOperationCoordinator.class);
        PaymentGateway gateway = mock(PaymentGateway.class);
        when(gateway.getProviderName()).thenReturn("MOCK");
        PaymentProperties properties = new PaymentProperties(); properties.setProvider("MOCK");
        PaymentService service = new PaymentService(coordinator, List.of(gateway), properties, new ObjectMapper());
        UUID customer = UUID.randomUUID(), order = UUID.randomUUID();
        Payment payment = Payment.createPending(UUID.randomUUID(), order, customer, new BigDecimal("10.00"), "USD", "same-key", "MOCK");
        when(coordinator.prepareAuthorization(eq(customer), any(), eq("MOCK"))).thenReturn(new PaymentOperationCoordinator.Preparation(payment, null, false));

        PaymentDtos.PaymentResponse response = service.authorizePayment(customer,
                new PaymentDtos.AuthorizePaymentRequest(order, new BigDecimal("10.00"), "USD", "same-key", "tok"));

        assertThat(response.status()).isEqualTo("PENDING");
        verify(gateway, never()).authorize(any(), any(), any(), any(), any());
    }

    @Test
    void paymentDomainRejectsInvalidStateTransitionsAndTracksPartialRefunds() {
        Payment payment = Payment.createPending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("20.00"), "USD", "k", "MOCK");
        payment.authorize("provider-payment");
        payment.capture(new BigDecimal("20.00"));
        payment.refund(new BigDecimal("5.00"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("5.00");
        payment.refund(new BigDecimal("15.00"));
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> payment.capture(new BigDecimal("1.00")))
                .isInstanceOf(IllegalStateException.class);
    }
}
