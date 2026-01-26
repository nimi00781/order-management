package com.articurated.ordermanagement.integration;

import com.articurated.ordermanagement.model.dto.request.CreateOrderRequest;
import com.articurated.ordermanagement.model.dto.request.OrderItemRequest;
import com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest;
import com.articurated.ordermanagement.model.dto.response.OrderResponse;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    private CreateOrderRequest createOrderRequest;

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
    }

    @Test
    void testCreateOrder_API() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderNumber").exists())
                .andExpect(jsonPath("$.customerId").value(100L))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andExpect(jsonPath("$.orderItems").isArray())
                .andExpect(jsonPath("$.orderItems[0].productName").value("Artisan Vase"))
                .andReturn();

        // Verify order was saved
        String responseContent = result.getResponse().getContentAsString();
        OrderResponse orderResponse = objectMapper.readValue(responseContent, OrderResponse.class);
        assertTrue(orderRepository.existsById(orderResponse.getId()));
    }

    @Test
    void testCreateOrder_ValidationError_MissingCustomerId() throws Exception {
        // Given
        createOrderRequest.setCustomerId(null);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.customerId").exists());
    }

    @Test
    void testCreateOrder_ValidationError_EmptyOrderItems() throws Exception {
        // Given
        createOrderRequest.setOrderItems(Arrays.asList());

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetOrderById_API() throws Exception {
        // Given - Create an order first
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse createdOrder = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderResponse.class);

        // When & Then
        mockMvc.perform(get("/api/orders/{orderId}", createdOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdOrder.getId()))
                .andExpect(jsonPath("$.orderNumber").value(createdOrder.getOrderNumber()))
                .andExpect(jsonPath("$.customerId").value(100L));
    }

    @Test
    void testGetOrderById_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/orders/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order Not Found"));
    }

    @Test
    void testGetOrderByOrderNumber_API() throws Exception {
        // Given - Create an order first
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse createdOrder = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderResponse.class);

        // When & Then
        mockMvc.perform(get("/api/orders/order-number/{orderNumber}", createdOrder.getOrderNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(createdOrder.getOrderNumber()));
    }

    @Test
    void testGetOrdersByCustomerId_API() throws Exception {
        // Given - Create multiple orders for same customer
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/orders/customer/{customerId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value(100L));
    }

    @Test
    void testUpdateOrderStatus_API() throws Exception {
        // Given - Create an order first
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse createdOrder = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderResponse.class);

        UpdateOrderStatusRequest updateRequest = new UpdateOrderStatusRequest();
        updateRequest.setNewStatus(OrderStatus.PAID);
        updateRequest.setReason("Payment received");

        // When & Then
        mockMvc.perform(put("/api/orders/{orderId}/status", createdOrder.getId())
                        .header("userId", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void testCancelOrder_API() throws Exception {
        // Given - Create an order first
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse createdOrder = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderResponse.class);

        // When & Then
        mockMvc.perform(post("/api/orders/{orderId}/cancel", createdOrder.getId())
                        .header("userId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testGetOrderStateHistory_API() throws Exception {
        // Given - Create an order and update its status
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse createdOrder = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderResponse.class);

        // When & Then
        mockMvc.perform(get("/api/orders/{orderId}/history", createdOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].entityType").value("ORDER"));
    }

    @Test
    void testCreateOrder_InvalidPrice() throws Exception {
        // Given
        createOrderRequest.getOrderItems().get(0).setPrice(new BigDecimal("-10.00"));

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_InvalidQuantity() throws Exception {
        // Given
        createOrderRequest.getOrderItems().get(0).setQuantity(0);

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isBadRequest());
    }
}
