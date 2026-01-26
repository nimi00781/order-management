package com.articurated.ordermanagement.model.exception;

public class BusinessRuleViolationException extends RuntimeException {
    private final String entityType;
    private final String rule;

    public BusinessRuleViolationException(String message) {
        super(message);
        this.entityType = null;
        this.rule = null;
    }

    public BusinessRuleViolationException(String entityType, String rule, String message) {
        super(message);
        this.entityType = entityType;
        this.rule = rule;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getRule() {
        return rule;
    }
}
