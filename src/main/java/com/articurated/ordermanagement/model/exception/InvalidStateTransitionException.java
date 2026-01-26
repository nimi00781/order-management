package com.articurated.ordermanagement.model.exception;

import com.articurated.ordermanagement.model.enums.EntityType;

public class InvalidStateTransitionException extends RuntimeException {
    private final String currentState;
    private final String targetState;
    private final EntityType entityType;

    public InvalidStateTransitionException(String currentState, String targetState, EntityType entityType) {
        super(String.format("Invalid state transition from %s to %s for entity type %s", 
                currentState, targetState, entityType));
        this.currentState = currentState;
        this.targetState = targetState;
        this.entityType = entityType;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getTargetState() {
        return targetState;
    }

    public EntityType getEntityType() {
        return entityType;
    }
}
