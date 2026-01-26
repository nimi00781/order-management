package com.articurated.ordermanagement.controller;

import com.articurated.ordermanagement.model.dto.request.CreateOrderRequest;
import com.articurated.ordermanagement.model.dto.request.OrderItemRequest;
import com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest;
import com.articurated.ordermanagement.model.dto.response.OrderResponse;
import com.articurated.ordermanagement.model.dto.response.StateHistoryResponse;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOrderRequest createOrderRequest;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCustomerId(100L);
        createOrderRequest.setShippingAddress("123 Main St, City, Country");
        
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(101L);
        itemRequest.setProductName("Artisan Vase");
        itemRequest.setQuantity(2);
        itemRequest.setPrice(new BigDecimal("150.00"));
        createOrderRequest.setOrderItems(Arrays.asList(itemRequest));

        orderResponse = OrderResponse.builder()
                .id(1L)
                .orderNumber("ORD-2024-ABC123")
                .customerId(100L)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("300.00"))
                .shippingAddress("123 Main St, City, Country")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateOrder_Success() throws Exception {
        // Given
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.orderNumber").value("ORD-2024-ABC123"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void testCreateOrder_ValidationError() throws Exception {
        // Given
        CreateOrderRequest invalidRequest = new CreateOrderRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetOrderById_Success() throws Exception {
        // Given
        when(orderService.getOrderById(1L)).thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.orderNumber").value("ORD-2024-ABC123"));
    }

    @Test
    void testGetOrderByOrderNumber_Success() throws Exception {
        // Given
        when(orderService.getOrderByOrderNumber("ORD-2024-ABC123")).thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(get("/api/orders/order-number/ORD-2024-ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-2024-ABC123"));
    }

    @Test
    void testGetOrdersByCustomerId_Success() throws Exception {
        // Given
        when(orderService.getOrdersByCustomerId(100L)).thenReturn(Arrays.asList(orderResponse));

        // When & Then
        mockMvc.perform(get("/api/orders/customer/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(100L));
    }

    @Test
    void testUpdateOrderStatus_Success() throws Exception {
        // Given
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setNewStatus(OrderStatus.PAID);
        request.setReason("Payment received");

        orderResponse.setStatus(OrderStatus.PAID);
        when(orderService.updateOrderStatus(eq(1L), any(UpdateOrderStatusRequest.class), eq(200L)))
                .thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(put("/api/orders/1/status")
                        .header("userId", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void testCancelOrder_Success() throws Exception {
        // Given
        orderResponse.setStatus(OrderStatus.CANCELLED);
        when(orderService.cancelOrder(1L, 200L)).thenReturn(orderResponse);

        // When & Then
        mockMvc.perform(post("/api/orders/1/cancel")
                        .header("userId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testGetOrderStateHistory_Success() throws Exception {
        // Given
        StateHistoryResponse historyResponse = StateHistoryResponse.builder()
                .id(1L)
                .entityId(1L)
                .fromState("PENDING_PAYMENT")
                .toState("PAID")
                .triggeredBy("100")
                .timestamp(LocalDateTime.now())
                .build();

        when(orderService.getOrderStateHistory(1L)).thenReturn(Arrays.asList(historyResponse));

        // When & Then
        mockMvc.perform(get("/api/orders/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromState").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$[0].toState").value("PAID"));
    }
}
