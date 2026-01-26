package com.articurated.ordermanagement.model.exception;

public class PaymentProcessingException extends RuntimeException {
    private final Long paymentId;
    private final String errorMessage;

    public PaymentProcessingException(Long paymentId, String errorMessage) {
        super("Payment processing failed for payment id: " + paymentId + ". Error: " + errorMessage);
        this.paymentId = paymentId;
        this.errorMessage = errorMessage;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
