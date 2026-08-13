package com.novacommerce.order.application;

import com.novacommerce.order.api.error.OrderException;
import com.novacommerce.order.domain.cart.Cart;
import com.novacommerce.order.domain.cart.CartStatus;
import com.novacommerce.order.domain.order.CustomerOrder;
import com.novacommerce.order.domain.order.OrderStatus;
import com.novacommerce.order.infrastructure.client.CatalogClient.Variant;
import com.novacommerce.order.infrastructure.persistence.CartRepository;
import com.novacommerce.order.infrastructure.persistence.OrderRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutTransactions {
    private final CartRepository carts;
    private final OrderRepository orders;
    private final CheckoutHasher hasher;
    private final Clock clock;

    public CheckoutTransactions(CartRepository carts, OrderRepository orders, CheckoutHasher hasher, Clock clock) {
        this.carts = carts;
        this.orders = orders;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot(UUID owner, UUID cartId) {
        Cart cart = carts.findById(cartId).filter(candidate -> candidate.getOwnerId().equals(owner))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "CART_NOT_FOUND", "Cart not found"));
        return Snapshot.of(cart, hasher.hash(owner, cart.getId(), cart.getItems()));
    }

    @Transactional(readOnly = true)
    public CustomerOrder existing(UUID owner, String key, String hash) {
        return orders.findByOwnerIdAndIdempotencyKey(owner, key).map(order -> {
            requireMatchingIntent(order, hash);
            return order;
        }).orElse(null);
    }

    @Transactional
    public CustomerOrder create(UUID owner, String key, Snapshot snapshot, List<Variant> variants) {
        CustomerOrder prior = orders.findByOwnerIdAndIdempotencyKey(owner, key).orElse(null);
        if (prior != null) {
            requireMatchingIntent(prior, snapshot.hash());
            return prior;
        }

        Cart cart = carts.lockOwned(snapshot.cartId(), owner)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "CART_NOT_FOUND", "Cart not found"));

        // Recheck after acquiring the cart lock. A concurrent request with the same key may
        // have committed while this transaction waited for the lock.
        prior = orders.findByOwnerIdAndIdempotencyKey(owner, key).orElse(null);
        if (prior != null) {
            requireMatchingIntent(prior, snapshot.hash());
            return prior;
        }
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "CART_CHECKOUT_IN_PROGRESS", "Cart checkout is already in progress");
        }
        if (!snapshot.matches(cart)) {
            throw error(HttpStatus.CONFLICT, "CART_CHANGED", "Cart changed during checkout");
        }
        if (variants.size() != snapshot.lines().size()) {
            throw error(HttpStatus.CONFLICT, "VARIANT_NOT_SELLABLE", "One or more variants are not sellable");
        }

        Set<String> currencies = new HashSet<>();
        variants.forEach(variant -> currencies.add(variant.price().currency()));
        if (currencies.size() != 1) {
            throw error(HttpStatus.CONFLICT, "MIXED_CURRENCY_NOT_SUPPORTED", "One order must use one currency");
        }

        CustomerOrder order = new CustomerOrder(owner, cart.getId(), key, snapshot.hash(),
                currencies.iterator().next(), clock.instant());
        Map<UUID, Line> lines = new HashMap<>();
        snapshot.lines().forEach(line -> lines.put(line.variantId(), line));
        for (Variant variant : variants) {
            Line line = lines.get(variant.variantId());
            if (line == null) {
                throw error(HttpStatus.CONFLICT, "CART_CHANGED", "Catalog result does not match cart");
            }
            order.addItem(variant.variantId(), variant.productId(), variant.productName(), variant.productSlug(),
                    variant.variantName(), variant.sku(), variant.attributes(), line.quantity(), variant.price().amount());
        }
        cart.beginCheckout(clock.instant());
        return orders.save(order);
    }

    @Transactional
    public CustomerOrder complete(UUID id, UUID reservation, Instant expiry) {
        CustomerOrder order = orders.findById(id).orElseThrow();
        if (order.getStatus() == OrderStatus.PENDING_INVENTORY) {
            order.inventoryReserved(reservation, expiry, clock.instant());
            carts.findById(order.getSourceCartId()).ifPresent(cart -> cart.convert(clock.instant()));
        }
        return order;
    }

    @Transactional
    public void inventoryFailed(UUID id) {
        orders.findById(id).ifPresent(order -> {
            order.cancel("INVENTORY_UNAVAILABLE", clock.instant());
            carts.findById(order.getSourceCartId()).ifPresent(cart -> cart.reopen(clock.instant()));
        });
    }

    @Transactional(readOnly = true)
    public CustomerOrder cancellationTarget(UUID owner, UUID id, boolean admin) {
        CustomerOrder order = orders.findById(id)
                .filter(candidate -> admin || candidate.getOwnerId().equals(owner))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT && order.getStatus() != OrderStatus.CANCELLED) {
            throw error(HttpStatus.CONFLICT, "ORDER_NOT_CANCELLABLE", "Order cannot be cancelled from its current state");
        }
        return order;
    }

    @Transactional
    public CustomerOrder cancel(UUID owner, UUID id, boolean admin) {
        CustomerOrder order = orders.findById(id)
                .filter(candidate -> admin || candidate.getOwnerId().equals(owner))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found"));
        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            order.cancel("CUSTOMER_CANCELLED", clock.instant());
        }
        return order;
    }

    private static void requireMatchingIntent(CustomerOrder order, String hash) {
        if (!order.getRequestHash().equals(hash)) {
            throw error(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "Idempotency key was reused");
        }
    }

    private static OrderException error(HttpStatus status, String code, String message) {
        return new OrderException(status, code, message);
    }

    public record Line(UUID variantId, long quantity) {}

    public record Snapshot(UUID cartId, long version, List<Line> lines, String hash) {
        static Snapshot of(Cart cart, String hash) {
            return new Snapshot(cart.getId(), cart.getVersion(), cart.getItems().stream()
                    .map(item -> new Line(item.getVariantId(), item.getQuantity()))
                    .sorted(Comparator.comparing(Line::variantId)).toList(), hash);
        }

        boolean matches(Cart cart) {
            return cart.getVersion() == version && lines.equals(of(cart, hash).lines());
        }
    }
}
