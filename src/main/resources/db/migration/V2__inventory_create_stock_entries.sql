CREATE TABLE inventory.inv_stock_entries (
    id            UUID PRIMARY KEY,
    part_id       UUID         NOT NULL REFERENCES inventory.inv_parts (id),
    quantity      INT          NOT NULL CHECK (quantity > 0),
    note          TEXT,
    registered_by UUID         NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);

CREATE INDEX idx_inv_stock_entries_part_id ON inventory.inv_stock_entries (part_id);
