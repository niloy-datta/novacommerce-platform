package com.novacommerce.inventory.domain.inventory;
import java.util.UUID;
public class InsufficientStockException extends RuntimeException { public InsufficientStockException(UUID id) { super("Insufficient stock for variant " + id); } }
