package com.articurated.ordermanagement.model.exception;

public class RefundNotFoundException extends RuntimeException {
    private final Long refundId;

    public RefundNotFoundException(Long refundId) {
        super(refundId != null ? "Refund not found with id: " + refundId : "Refund not found");
        this.refundId = refundId;
    }

    public RefundNotFoundException(String message) {
        super(message);
        this.refundId = null;
    }

    public Long getRefundId() {
        return refundId;
    }
}
