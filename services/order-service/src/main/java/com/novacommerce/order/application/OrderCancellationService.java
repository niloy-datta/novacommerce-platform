package com.novacommerce.order.application;

import com.novacommerce.order.api.error.OrderException;
import com.novacommerce.order.domain.order.CustomerOrder;
import com.novacommerce.order.domain.order.OrderStatus;
import com.novacommerce.order.infrastructure.client.InventoryClient;
import com.novacommerce.order.infrastructure.client.InventoryClient.UnknownInventoryResultException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderCancellationService {
    private final CheckoutTransactions transactions;
    private final InventoryClient inventory;

    public OrderCancellationService(CheckoutTransactions transactions, InventoryClient inventory) {
        this.transactions = transactions;
        this.inventory = inventory;
    }

    public CustomerOrder cancel(UUID owner, UUID orderId, boolean admin, String accessToken) {
        CustomerOrder order = transactions.cancellationTarget(owner, orderId, admin);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        try {
            inventory.release(order.getReservationId(), accessToken);
        } catch (UnknownInventoryResultException exception) {
            throw new OrderException(HttpStatus.SERVICE_UNAVAILABLE, "INVENTORY_RELEASE_UNKNOWN",
                    "Cancellation is not confirmed; retry the same request safely");
        }
        return transactions.cancel(owner, orderId, admin);
    }
}
