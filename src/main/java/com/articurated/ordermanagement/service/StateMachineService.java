package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.Return;
import com.articurated.ordermanagement.model.enums.EntityType;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
import com.articurated.ordermanagement.model.exception.InvalidStateTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StateMachineService {
    private final StateHistoryService stateHistoryService;

    @Transactional
    public Order transitionOrderState(Order order, OrderStatus targetStatus, Long userId, String reason) {
        OrderStatus currentStatus = order.getStatus();
        
        if (!validateOrderTransition(currentStatus, targetStatus)) {
            throw new InvalidStateTransitionException(
                    currentStatus.name(), targetStatus.name(), EntityType.ORDER);
        }

        order.setStatus(targetStatus);
        
        stateHistoryService.logStateTransition(
                EntityType.ORDER,
                order.getId(),
                currentStatus.name(),
                targetStatus.name(),
                userId,
                reason
        );

        return order;
    }

    @Transactional
    public Return transitionReturnState(Return returnEntity, ReturnStatus targetStatus, Long userId, String reason) {
        ReturnStatus currentStatus = returnEntity.getStatus();
        
        if (!validateReturnTransition(currentStatus, targetStatus)) {
            throw new InvalidStateTransitionException(
                    currentStatus.name(), targetStatus.name(), EntityType.RETURN);
        }

        returnEntity.setStatus(targetStatus);
        
        // Update timestamps based on state
        if (targetStatus == ReturnStatus.APPROVED) {
            returnEntity.setApprovedAt(java.time.LocalDateTime.now());
            returnEntity.setApprovedBy(userId);
        } else if (targetStatus == ReturnStatus.IN_TRANSIT) {
            returnEntity.setShippedAt(java.time.LocalDateTime.now());
        } else if (targetStatus == ReturnStatus.RECEIVED) {
            returnEntity.setReceivedAt(java.time.LocalDateTime.now());
        }

        stateHistoryService.logStateTransition(
                EntityType.RETURN,
                returnEntity.getId(),
                currentStatus.name(),
                targetStatus.name(),
                userId,
                reason
        );

        return returnEntity;
    }

    public boolean validateOrderTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING_PAYMENT -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
            case PAID -> to == OrderStatus.PROCESSING_IN_WAREHOUSE || to == OrderStatus.CANCELLED;
            case PROCESSING_IN_WAREHOUSE -> to == OrderStatus.SHIPPED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false; // Terminal states
        };
    }

    public boolean validateReturnTransition(ReturnStatus from, ReturnStatus to) {
        return switch (from) {
            case REQUESTED -> to == ReturnStatus.APPROVED || to == ReturnStatus.REJECTED;
            case APPROVED -> to == ReturnStatus.IN_TRANSIT;
            case IN_TRANSIT -> to == ReturnStatus.RECEIVED;
            case RECEIVED -> to == ReturnStatus.COMPLETED;
            case REJECTED, COMPLETED -> false; // Terminal states
        };
    }

    public List<OrderStatus> getAllowedOrderTransitions(OrderStatus currentStatus) {
        return Arrays.stream(OrderStatus.values())
                .filter(status -> validateOrderTransition(currentStatus, status))
                .toList();
    }

    public List<ReturnStatus> getAllowedReturnTransitions(ReturnStatus currentStatus) {
        return Arrays.stream(ReturnStatus.values())
                .filter(status -> validateReturnTransition(currentStatus, status))
                .toList();
    }
}
