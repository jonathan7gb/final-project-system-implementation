CREATE TABLE orders.order_items (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    order_id    UUID         NOT NULL,
    part_id     UUID         NOT NULL,
    part_code   VARCHAR(50)  NOT NULL,
    part_name   VARCHAR(150) NOT NULL,
    quantity    INT          NOT NULL,

    CONSTRAINT pk_order_items           PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order     FOREIGN KEY (order_id) REFERENCES orders.orders (id),
    CONSTRAINT chk_order_items_qty      CHECK (quantity > 0)
);

CREATE INDEX idx_order_items_order_id ON orders.order_items (order_id);
