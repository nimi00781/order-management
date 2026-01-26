package com.articurated.ordermanagement.model.exception;

public class ReturnNotFoundException extends RuntimeException {
    private final Long returnId;

    public ReturnNotFoundException(Long returnId) {
        super("Return not found with id: " + returnId);
        this.returnId = returnId;
    }

    public Long getReturnId() {
        return returnId;
    }
}
