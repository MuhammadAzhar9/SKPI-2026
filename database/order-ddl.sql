-- Order Service DDL
-- Database: order_db
-- Run this on order_db as order_user

CREATE TABLE IF NOT EXISTS orders (
    id             BIGSERIAL       PRIMARY KEY,
    customer_name  VARCHAR(255)    NOT NULL,
    customer_email VARCHAR(255)    NOT NULL,
    status         VARCHAR(50)     NOT NULL,
    total_amount   NUMERIC(19, 2)  NOT NULL,
    created_at     TIMESTAMP       NOT NULL,
    updated_at     TIMESTAMP       NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id           BIGSERIAL       PRIMARY KEY,
    order_id     BIGINT          NOT NULL,
    product_id   BIGINT          NOT NULL,
    product_name VARCHAR(255)    NOT NULL,
    unit_price   NUMERIC(19, 2)  NOT NULL,
    quantity     INTEGER         NOT NULL,
    subtotal     NUMERIC(19, 2)  NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE IF NOT EXISTS idempotency_records (
    id              BIGSERIAL       PRIMARY KEY,
    idempotency_key VARCHAR(255)    NOT NULL,
    order_id        BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL,
    CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_idempotency_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- IMPORTANT: order_items.product_id is intentionally NOT a foreign key to catalog_db.
-- Product data (name, price) is stored as a snapshot at the time the order is created.
-- Order Service must NOT query catalog_db directly.
