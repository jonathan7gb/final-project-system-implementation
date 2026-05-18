CREATE TABLE inventory.inv_processed_order_events (
    order_id     UUID         NOT NULL,
    event_type   VARCHAR(50)  NOT NULL,
    processed_at TIMESTAMP    NOT NULL,
    PRIMARY KEY (order_id, event_type)
);
