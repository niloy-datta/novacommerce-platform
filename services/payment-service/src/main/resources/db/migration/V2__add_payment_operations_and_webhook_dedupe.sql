ALTER TABLE payments
    ADD COLUMN authorized_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN captured_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN refunded_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PENDING','AUTHORIZED','CAPTURED','FAILED','CANCELLED','PARTIALLY_REFUNDED','REFUNDED'));
ALTER TABLE payments ADD CONSTRAINT payments_amounts_valid
    CHECK (amount > 0 AND authorized_amount >= 0 AND captured_amount >= 0 AND refunded_amount >= 0
           AND authorized_amount <= amount AND captured_amount <= authorized_amount
           AND refunded_amount <= captured_amount);

CREATE TABLE payment_operations (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    operation_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    amount NUMERIC(19,2),
    provider_operation_id VARCHAR(128),
    failure_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT payment_operation_type_check CHECK (operation_type IN ('AUTHORIZE','CAPTURE','CANCEL','REFUND')),
    CONSTRAINT payment_operation_status_check CHECK (status IN ('PENDING','SUCCEEDED','FAILED','UNKNOWN')),
    CONSTRAINT payment_operation_amount_check CHECK (amount IS NULL OR amount > 0)
);
CREATE INDEX payment_operations_payment_created_idx ON payment_operations(payment_id, created_at DESC);
CREATE UNIQUE INDEX payment_operations_single_capture_idx ON payment_operations(payment_id)
    WHERE operation_type = 'CAPTURE';
CREATE UNIQUE INDEX payment_operations_single_cancel_idx ON payment_operations(payment_id)
    WHERE operation_type = 'CANCEL';

CREATE TABLE processed_webhook_events (
    provider_event_id VARCHAR(200) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);
