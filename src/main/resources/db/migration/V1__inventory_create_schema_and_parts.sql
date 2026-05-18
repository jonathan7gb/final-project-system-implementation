CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE inventory.inv_parts (
    id           UUID PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    name         VARCHAR(150) NOT NULL,
    unit         VARCHAR(20)  NOT NULL,
    qty_in_stock INT          NOT NULL DEFAULT 0 CHECK (qty_in_stock >= 0),
    qty_reserved INT          NOT NULL DEFAULT 0 CHECK (qty_reserved >= 0),
    qty_minimum  INT          NOT NULL DEFAULT 0 CHECK (qty_minimum >= 0),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);
