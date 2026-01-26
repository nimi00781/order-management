package com.articurated.ordermanagement.model.exception;

public class OrderNotFoundException extends RuntimeException {
    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
