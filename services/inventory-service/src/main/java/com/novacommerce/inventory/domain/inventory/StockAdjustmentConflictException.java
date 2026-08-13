package com.novacommerce.inventory.domain.inventory;
import java.util.UUID;
public class StockAdjustmentConflictException extends RuntimeException { public StockAdjustmentConflictException(UUID id) { super("Adjustment conflicts with reserved stock for variant " + id); } }
