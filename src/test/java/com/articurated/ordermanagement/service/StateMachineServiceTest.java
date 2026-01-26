package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.Return;
import com.articurated.ordermanagement.model.enums.EntityType;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
import com.articurated.ordermanagement.model.exception.InvalidStateTransitionException;
import com.articurated.ordermanagement.repository.OrderRepository;
import com.articurated.ordermanagement.repository.ReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateMachineServiceTest {

    @Mock
    private StateHistoryService stateHistoryService;

    @InjectMocks
    private StateMachineService stateMachineService;

    private Order testOrder;
    private Return testReturn;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-2024-ABC123")
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("300.00"))
                .build();

        testReturn = Return.builder()
                .id(1L)
                .returnReason("Item damaged")
                .status(ReturnStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testTransitionOrderState_Success() {
        // Given
        com.articurated.ordermanagement.model.entity.StateTransition transition = 
                com.articurated.ordermanagement.model.entity.StateTransition.builder()
                        .id(1L)
                        .entityType(EntityType.ORDER)
                        .entityId(1L)
                        .fromState("PENDING_PAYMENT")
                        .toState("PAID")
                        .build();
        when(stateHistoryService.logStateTransition(any(), any(), any(), any(), any(), any())).thenReturn(transition);

        // When
        Order result = stateMachineService.transitionOrderState(
                testOrder, OrderStatus.PAID, 100L, "Payment received");

        // Then
        assertNotNull(result);
        assertEquals(OrderStatus.PAID, result.getStatus());
        verify(stateHistoryService, times(1)).logStateTransition(
                eq(EntityType.ORDER), eq(1L), eq("PENDING_PAYMENT"), 
                eq("PAID"), eq(100L), eq("Payment received"));
    }

    @Test
    void testTransitionOrderState_InvalidTransition() {
        // When & Then
        assertThrows(InvalidStateTransitionException.class, () -> 
                stateMachineService.transitionOrderState(
                        testOrder, OrderStatus.DELIVERED, 100L, "Invalid"));
        verify(stateHistoryService, never()).logStateTransition(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testTransitionReturnState_Success() {
        // Given
        com.articurated.ordermanagement.model.entity.StateTransition transition = 
                com.articurated.ordermanagement.model.entity.StateTransition.builder()
                        .id(1L)
                        .entityType(EntityType.RETURN)
                        .entityId(1L)
                        .fromState("REQUESTED")
                        .toState("APPROVED")
                        .build();
        when(stateHistoryService.logStateTransition(any(), any(), any(), any(), any(), any())).thenReturn(transition);

        // When
        Return result = stateMachineService.transitionReturnState(
                testReturn, ReturnStatus.APPROVED, 200L, "Approved by manager");

        // Then
        assertNotNull(result);
        assertEquals(ReturnStatus.APPROVED, result.getStatus());
        assertNotNull(result.getApprovedAt());
        assertEquals(200L, result.getApprovedBy());
        verify(stateHistoryService, times(1)).logStateTransition(
                eq(EntityType.RETURN), eq(1L), eq("REQUESTED"), 
                eq("APPROVED"), eq(200L), eq("Approved by manager"));
    }

    @Test
    void testTransitionReturnState_InvalidTransition() {
        // When & Then
        assertThrows(InvalidStateTransitionException.class, () -> 
                stateMachineService.transitionReturnState(
                        testReturn, ReturnStatus.COMPLETED, 200L, "Invalid"));
        verify(stateHistoryService, never()).logStateTransition(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testValidateOrderTransition_ValidTransitions() {
        // Test valid transitions
        assertTrue(stateMachineService.validateOrderTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID));
        assertTrue(stateMachineService.validateOrderTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED));
        assertTrue(stateMachineService.validateOrderTransition(OrderStatus.PAID, OrderStatus.PROCESSING_IN_WAREHOUSE));
        assertTrue(stateMachineService.validateOrderTransition(OrderStatus.PAID, OrderStatus.CANCELLED));
        assertTrue(stateMachineService.validateOrderTransition(OrderStatus.PROCESSING_IN_WAREHOUSE, OrderStatus.SHIPPED));
        assertTrue(stateMachineService.validateOrderTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED));
    }

    @Test
    void testValidateOrderTransition_InvalidTransitions() {
        // Test invalid transitions
        assertFalse(stateMachineService.validateOrderTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.SHIPPED));
        assertFalse(stateMachineService.validateOrderTransition(OrderStatus.PAID, OrderStatus.DELIVERED));
        assertFalse(stateMachineService.validateOrderTransition(OrderStatus.DELIVERED, OrderStatus.PAID));
        assertFalse(stateMachineService.validateOrderTransition(OrderStatus.CANCELLED, OrderStatus.PAID));
    }

    @Test
    void testValidateReturnTransition_ValidTransitions() {
        // Test valid transitions
        assertTrue(stateMachineService.validateReturnTransition(ReturnStatus.REQUESTED, ReturnStatus.APPROVED));
        assertTrue(stateMachineService.validateReturnTransition(ReturnStatus.REQUESTED, ReturnStatus.REJECTED));
        assertTrue(stateMachineService.validateReturnTransition(ReturnStatus.APPROVED, ReturnStatus.IN_TRANSIT));
        assertTrue(stateMachineService.validateReturnTransition(ReturnStatus.IN_TRANSIT, ReturnStatus.RECEIVED));
        assertTrue(stateMachineService.validateReturnTransition(ReturnStatus.RECEIVED, ReturnStatus.COMPLETED));
    }

    @Test
    void testValidateReturnTransition_InvalidTransitions() {
        // Test invalid transitions
        assertFalse(stateMachineService.validateReturnTransition(ReturnStatus.REQUESTED, ReturnStatus.COMPLETED));
        assertFalse(stateMachineService.validateReturnTransition(ReturnStatus.APPROVED, ReturnStatus.REQUESTED));
        assertFalse(stateMachineService.validateReturnTransition(ReturnStatus.REJECTED, ReturnStatus.APPROVED));
        assertFalse(stateMachineService.validateReturnTransition(ReturnStatus.COMPLETED, ReturnStatus.RECEIVED));
    }

    @Test
    void testGetAllowedOrderTransitions() {
        // When
        List<OrderStatus> allowed = stateMachineService.getAllowedOrderTransitions(OrderStatus.PENDING_PAYMENT);

        // Then
        assertNotNull(allowed);
        assertTrue(allowed.contains(OrderStatus.PAID));
        assertTrue(allowed.contains(OrderStatus.CANCELLED));
        assertEquals(2, allowed.size());
    }

    @Test
    void testGetAllowedReturnTransitions() {
        // When
        List<ReturnStatus> allowed = stateMachineService.getAllowedReturnTransitions(ReturnStatus.REQUESTED);

        // Then
        assertNotNull(allowed);
        assertTrue(allowed.contains(ReturnStatus.APPROVED));
        assertTrue(allowed.contains(ReturnStatus.REJECTED));
        assertEquals(2, allowed.size());
    }
}
