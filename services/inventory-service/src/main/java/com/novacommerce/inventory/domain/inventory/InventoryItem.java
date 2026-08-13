package com.novacommerce.inventory.domain.inventory;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "inventory_items")
public class InventoryItem {
    @Id @Column(name = "variant_id") private UUID variantId;
    @Column(name = "on_hand", nullable = false) private long onHand;
    @Column(nullable = false) private long reserved;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected InventoryItem() { }
    public InventoryItem(UUID variantId, long onHand, Instant now) {
        if (variantId == null || onHand < 0) throw new IllegalArgumentException("Invalid inventory item");
        this.variantId = variantId; this.onHand = onHand; this.createdAt = this.updatedAt = now;
    }
    public long available() { return onHand - reserved; }
    public void reserve(long quantity, Instant now) { positive(quantity); if (available() < quantity) throw new InsufficientStockException(variantId); reserved += quantity; updatedAt = now; }
    public void release(long quantity, Instant now) { positive(quantity); if (reserved < quantity) throw new IllegalStateException("Reserved quantity underflow"); reserved -= quantity; updatedAt = now; }
    public void commit(long quantity, Instant now) { positive(quantity); if (reserved < quantity) throw new IllegalStateException("Reserved quantity underflow"); reserved -= quantity; onHand -= quantity; updatedAt = now; }
    public void adjust(long delta, Instant now) { if (delta == 0) throw new IllegalArgumentException("Quantity delta must not be zero"); if (onHand + delta < reserved) throw new StockAdjustmentConflictException(variantId); onHand += delta; updatedAt = now; }
    private static void positive(long quantity) { if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive"); }
    public UUID getVariantId() { return variantId; } public long getOnHand() { return onHand; } public long getReserved() { return reserved; } public long getVersion() { return version; }
}
