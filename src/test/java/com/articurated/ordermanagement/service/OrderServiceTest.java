package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.model.dto.request.CreateOrderRequest;
import com.articurated.ordermanagement.model.dto.request.OrderItemRequest;
import com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest;
import com.articurated.ordermanagement.model.dto.response.OrderResponse;
import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.OrderItem;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.exception.BusinessRuleViolationException;
import com.articurated.ordermanagement.model.exception.OrderNotFoundException;
import com.articurated.ordermanagement.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StateMachineService stateMachineService;

    @Mock
    private StateHistoryService stateHistoryService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-2024-ABC123")
                .customerId(100L)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("300.00"))
                .shippingAddress("123 Main St, City, Country")
                .orderItems(Arrays.asList(
                        OrderItem.builder()
                                .id(1L)
                                .productId(101L)
                                .productName("Artisan Vase")
                                .quantity(2)
                                .price(new BigDecimal("150.00"))
                                .subtotal(new BigDecimal("300.00"))
                                .build()
                ))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCustomerId(100L);
        createOrderRequest.setShippingAddress("123 Main St, City, Country");
        
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(101L);
        itemRequest.setProductName("Artisan Vase");
        itemRequest.setQuantity(2);
        itemRequest.setPrice(new BigDecimal("150.00"));
        
        createOrderRequest.setOrderItems(Arrays.asList(itemRequest));
    }

    @Test
    void testCreateOrder_Success() {
        // Given
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setOrderNumber("ORD-2024-ABC123");
            return order;
        });

        // When
        OrderResponse response = orderService.createOrder(createOrderRequest);

        // Then
        assertNotNull(response);
        assertEquals(100L, response.getCustomerId());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.getStatus());
        assertEquals("123 Main St, City, Country", response.getShippingAddress());
        assertNotNull(response.getOrderItems());
        assertEquals(1, response.getOrderItems().size());
        
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(stateHistoryService, times(1)).logStateTransition(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testGetOrderById_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        OrderResponse response = orderService.getOrderById(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("ORD-2024-ABC123", response.getOrderNumber());
        assertEquals(OrderStatus.PENDING_PAYMENT, response.getStatus());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrderById_NotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(999L));
        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    void testGetOrderByOrderNumber_Success() {
        // Given
        when(orderRepository.findByOrderNumber("ORD-2024-ABC123")).thenReturn(Optional.of(testOrder));

        // When
        OrderResponse response = orderService.getOrderByOrderNumber("ORD-2024-ABC123");

        // Then
        assertNotNull(response);
        assertEquals("ORD-2024-ABC123", response.getOrderNumber());
        verify(orderRepository, times(1)).findByOrderNumber("ORD-2024-ABC123");
    }

    @Test
    void testGetOrdersByCustomerId_Success() {
        // Given
        when(orderRepository.findByCustomerId(100L)).thenReturn(Arrays.asList(testOrder));

        // When
        List<OrderResponse> responses = orderService.getOrdersByCustomerId(100L);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(100L, responses.get(0).getCustomerId());
        verify(orderRepository, times(1)).findByCustomerId(100L);
    }

    @Test
    void testUpdateOrderStatus_Success() {
        // Given
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setNewStatus(OrderStatus.PAID);
        request.setReason("Payment received");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(stateMachineService.transitionOrderState(any(), any(), any(), any())).thenReturn(testOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderResponse response = orderService.updateOrderStatus(1L, request, 200L);

        // Then
        assertNotNull(response);
        verify(stateMachineService, times(1)).transitionOrderState(
                any(Order.class), eq(OrderStatus.PAID), eq(200L), eq("Payment received"));
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(emailService, times(1)).sendOrderStatusUpdate(any(), eq(OrderStatus.PAID));
    }

    @Test
    void testCancelOrder_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(stateMachineService.transitionOrderState(any(), any(), any(), any())).thenReturn(testOrder);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderResponse response = orderService.cancelOrder(1L, 200L);

        // Then
        assertNotNull(response);
        verify(stateMachineService, times(1)).transitionOrderState(
                any(Order.class), eq(OrderStatus.CANCELLED), eq(200L), anyString());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCancelOrder_CannotBeCancelled() {
        // Given
        testOrder.setStatus(OrderStatus.PROCESSING_IN_WAREHOUSE);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(BusinessRuleViolationException.class, () -> orderService.cancelOrder(1L, 200L));
        verify(orderRepository, times(1)).findById(1L);
        verify(stateMachineService, never()).transitionOrderState(any(), any(), any(), any());
    }

    @Test
    void testGetOrderStateHistory_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        List<com.articurated.ordermanagement.model.dto.response.StateHistoryResponse> history = 
                orderService.getOrderStateHistory(1L);

        // Then
        assertNotNull(history);
        verify(stateHistoryService, times(1)).getStateHistory(any(), eq(1L));
    }
}
