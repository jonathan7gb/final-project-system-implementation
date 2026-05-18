CREATE TABLE inventory.stock_entries (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    part_id         UUID        NOT NULL,
    quantity        INT         NOT NULL,
    note            TEXT,
    registered_by   UUID        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_stock_entries          PRIMARY KEY (id),
    CONSTRAINT fk_stock_entries_part     FOREIGN KEY (part_id) REFERENCES inventory.parts (id),
    CONSTRAINT chk_stock_entries_qty     CHECK (quantity > 0)
);

CREATE INDEX idx_stock_entries_part_id ON inventory.stock_entries (part_id);
