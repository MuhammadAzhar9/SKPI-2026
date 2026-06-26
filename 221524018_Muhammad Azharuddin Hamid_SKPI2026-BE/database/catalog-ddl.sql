-- Catalog Service DDL
-- Database: catalog_db
-- Run this on catalog_db as catalog_user

CREATE TABLE IF NOT EXISTS products (
    id          BIGSERIAL       PRIMARY KEY,
    sku         VARCHAR(255)    NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    price       NUMERIC(19, 2)  NOT NULL,
    stock       INTEGER         NOT NULL,
    status      VARCHAR(50)     NOT NULL,
    created_at  TIMESTAMP       NOT NULL,
    updated_at  TIMESTAMP       NOT NULL,
    CONSTRAINT uk_products_sku UNIQUE (sku)
);
