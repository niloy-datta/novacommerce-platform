package com.novacommerce.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novacommerce.order.domain.order.CustomerOrder;
import com.novacommerce.order.domain.order.OrderStatus;
import com.novacommerce.order.infrastructure.client.CatalogClient;
import com.novacommerce.order.infrastructure.client.InventoryClient;
import com.novacommerce.order.infrastructure.client.InventoryClient.UnknownInventoryResultException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckoutRecoveryTests {
    @Test
    void retryUsesSameOrderAndInventoryIdempotencyIdentityAfterUnknownResult() {
        CheckoutTransactions transactions = mock(CheckoutTransactions.class);
        CatalogClient catalog = mock(CatalogClient.class);
        InventoryClient inventory = mock(InventoryClient.class);
        CheckoutService service = new CheckoutService(transactions, catalog, inventory);
        UUID owner = UUID.randomUUID();
        UUID cart = UUID.randomUUID();
        UUID variant = UUID.randomUUID();
        UUID reservation = UUID.randomUUID();
        CheckoutTransactions.Snapshot snapshot = new CheckoutTransactions.Snapshot(cart, 1,
                List.of(new CheckoutTransactions.Line(variant, 2)), "intent-hash");
        CustomerOrder order = new CustomerOrder(owner, cart, "checkout-key", "intent-hash", "USD", Instant.now());
        order.addItem(variant, UUID.randomUUID(), "Product", "product", "Default", "SKU-1", Map.of(), 2,
                new BigDecimal("12.50"));
        CatalogClient.Variant catalogVariant = new CatalogClient.Variant(variant, UUID.randomUUID(), "Product",
                "product", "Default", "SKU-1", Map.of(), new CatalogClient.Price(new BigDecimal("12.50"), "USD"));

        when(transactions.snapshot(owner, cart)).thenReturn(snapshot);
        when(transactions.existing(owner, "checkout-key", "intent-hash")).thenReturn(null, order);
        when(catalog.variants(List.of(variant))).thenReturn(List.of(catalogVariant));
        when(transactions.create(owner, "checkout-key", snapshot, List.of(catalogVariant))).thenReturn(order);
        when(inventory.reserve(eq(order.getId()), any(), eq("jwt")))
                .thenThrow(new UnknownInventoryResultException(new RuntimeException("connection reset")))
                .thenReturn(new InventoryClient.Reservation(reservation, "ACTIVE", Instant.now().plusSeconds(900), List.of()));
        when(transactions.complete(eq(order.getId()), eq(reservation), any())).thenAnswer(invocation -> {
            order.inventoryReserved(reservation, invocation.getArgument(2), Instant.now());
            return order;
        });

        CheckoutService.Result first = service.checkout(owner, cart, "checkout-key", "jwt");
        CheckoutService.Result retry = service.checkout(owner, cart, "checkout-key", "jwt");

        assertThat(first.retryable()).isTrue();
        assertThat(first.orderId()).isEqualTo(order.getId());
        assertThat(retry.orderId()).isEqualTo(order.getId());
        assertThat(retry.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        verify(inventory, org.mockito.Mockito.times(2)).reserve(eq(order.getId()), any(), eq("jwt"));
    }
}
