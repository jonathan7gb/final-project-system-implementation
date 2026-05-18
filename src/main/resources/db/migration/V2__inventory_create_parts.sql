CREATE TABLE inventory.parts (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(150) NOT NULL,
    unit         VARCHAR(20)  NOT NULL,
    qty_in_stock INT          NOT NULL DEFAULT 0,
    qty_reserved INT          NOT NULL DEFAULT 0,
    qty_minimum  INT          NOT NULL DEFAULT 0,
    active       BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT pk_parts              PRIMARY KEY (id),
    CONSTRAINT uq_parts_code         UNIQUE (code),
    CONSTRAINT chk_parts_in_stock    CHECK (qty_in_stock >= 0),
    CONSTRAINT chk_parts_reserved    CHECK (qty_reserved >= 0),
    CONSTRAINT chk_parts_minimum     CHECK (qty_minimum >= 0)
);
