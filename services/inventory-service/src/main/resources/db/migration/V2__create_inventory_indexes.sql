CREATE INDEX inventory_reservations_status_expiry_idx ON inventory_reservations (status, expires_at);
CREATE INDEX inventory_reservation_items_variant_idx ON inventory_reservation_items (variant_id);
CREATE INDEX inventory_movements_variant_created_idx ON inventory_movements (variant_id, created_at DESC);
