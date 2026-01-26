package com.articurated.ordermanagement.model.exception;

public class PaymentNotFoundException extends RuntimeException {
    private final Long paymentId;

    public PaymentNotFoundException(Long paymentId) {
        super("Payment not found with id: " + paymentId);
        this.paymentId = paymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}
