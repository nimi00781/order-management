-- Create orders table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    shipping_address VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT chk_total_amount CHECK (total_amount > 0),
    CONSTRAINT chk_order_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING_IN_WAREHOUSE', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

-- Create indexes for orders
CREATE INDEX idx_order_number ON orders(order_number);
CREATE INDEX idx_customer_id ON orders(customer_id);
CREATE INDEX idx_status ON orders(status);

-- Create order_items table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    subtotal NUMERIC(19, 2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_price CHECK (price >= 0)
);

-- Create payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255) UNIQUE,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

-- Create indexes for payments
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_status ON payments(status);

-- Create returns table
CREATE TABLE returns (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    return_reason VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    shipped_at TIMESTAMP,
    received_at TIMESTAMP,
    comments VARCHAR(1000),
    CONSTRAINT fk_returns_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_return_status CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'IN_TRANSIT', 'RECEIVED', 'COMPLETED'))
);

-- Create indexes for returns
CREATE INDEX idx_returns_order_id ON returns(order_id);
CREATE INDEX idx_returns_status ON returns(status);

-- Create refunds table
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    return_id BIGINT NOT NULL UNIQUE,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    refund_method VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(255) UNIQUE,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_refunds_return FOREIGN KEY (return_id) REFERENCES returns(id) ON DELETE CASCADE,
    CONSTRAINT chk_refund_amount CHECK (amount > 0),
    CONSTRAINT chk_refund_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

-- Create indexes for refunds
CREATE INDEX idx_refunds_return_id ON refunds(return_id);
CREATE INDEX idx_refunds_status ON refunds(status);

-- Create state_transitions table (audit log)
CREATE TABLE state_transitions (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    from_state VARCHAR(50),
    to_state VARCHAR(50) NOT NULL,
    triggered_by VARCHAR(100) NOT NULL,
    transition_reason VARCHAR(500),
    timestamp TIMESTAMP NOT NULL,
    CONSTRAINT chk_entity_type CHECK (entity_type IN ('ORDER', 'RETURN'))
);

-- Create indexes for state_transitions
CREATE INDEX idx_entity_type_id ON state_transitions(entity_type, entity_id);
CREATE INDEX idx_timestamp ON state_transitions(timestamp);
