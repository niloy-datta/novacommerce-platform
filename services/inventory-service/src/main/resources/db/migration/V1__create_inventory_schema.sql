CREATE TABLE inventory_items (
    variant_id UUID PRIMARY KEY,
    on_hand BIGINT NOT NULL,
    reserved BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT inventory_non_negative CHECK (on_hand >= 0 AND reserved >= 0),
    CONSTRAINT inventory_reserved_within_stock CHECK (reserved <= on_hand)
);

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    request_hash VARCHAR(64) NOT NULL,
    owner_id UUID,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT inventory_reservation_status CHECK (status IN ('ACTIVE','COMMITTED','RELEASED','EXPIRED'))
);

CREATE TABLE inventory_reservation_items (
    reservation_id UUID NOT NULL REFERENCES inventory_reservations(id),
    variant_id UUID NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (reservation_id, variant_id)
);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    variant_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    on_hand_delta BIGINT NOT NULL,
    reserved_delta BIGINT NOT NULL,
    reference_id UUID,
    reason TEXT,
    actor_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);
