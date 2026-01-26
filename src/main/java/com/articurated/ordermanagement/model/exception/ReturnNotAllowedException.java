package com.articurated.ordermanagement.model.exception;

public class ReturnNotAllowedException extends RuntimeException {
    private final Long orderId;
    private final String reason;

    public ReturnNotAllowedException(Long orderId, String reason) {
        super("Return not allowed for order id: " + orderId + ". Reason: " + reason);
        this.orderId = orderId;
        this.reason = reason;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getReason() {
        return reason;
    }
}
