CREATE TYPE orders.order_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED',
    'COMPLETED'
);

CREATE TABLE orders.orders (
    id              UUID                 NOT NULL DEFAULT gen_random_uuid(),
    requester_id    UUID                 NOT NULL,
    status          orders.order_status  NOT NULL DEFAULT 'PENDING',
    justification   TEXT                 NOT NULL,
    rejection_note  TEXT,
    reviewed_by     UUID,
    reviewed_at     TIMESTAMP,
    created_at      TIMESTAMP            NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP            NOT NULL DEFAULT now(),

    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE INDEX idx_orders_requester_id ON orders.orders (requester_id);
CREATE INDEX idx_orders_status       ON orders.orders (status);
