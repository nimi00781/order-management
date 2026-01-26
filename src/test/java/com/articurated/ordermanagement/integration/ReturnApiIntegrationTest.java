package com.articurated.ordermanagement.integration;

import com.articurated.ordermanagement.model.dto.request.ApproveReturnRequest;
import com.articurated.ordermanagement.model.dto.request.CreateOrderRequest;
import com.articurated.ordermanagement.model.dto.request.CreateReturnRequest;
import com.articurated.ordermanagement.model.dto.request.OrderItemRequest;
import com.articurated.ordermanagement.model.dto.response.OrderResponse;
import com.articurated.ordermanagement.model.dto.response.ReturnResponse;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReturnApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long orderId;
    private CreateReturnRequest createReturnRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Create an order and set it to DELIVERED status
        CreateOrderRequest createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCustomerId(100L);
        createOrderRequest.setShippingAddress("123 Main St, City, Country");
        
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(101L);
        itemRequest.setProductName("Artisan Vase");
        itemRequest.setQuantity(2);
        itemRequest.setPrice(new BigDecimal("150.00"));
        createOrderRequest.setOrderItems(Arrays.asList(itemRequest));

        // Create order
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse orderResponse = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderResponse.class);
        orderId = orderResponse.getId();

        // Update order to DELIVERED status through proper state transitions
        // PENDING_PAYMENT -> PAID
        com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest updateRequest1 = 
                new com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest();
        updateRequest1.setNewStatus(OrderStatus.PAID);
        updateRequest1.setReason("Payment received");
        mockMvc.perform(put("/api/orders/{orderId}/status", orderId)
                        .header("userId", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest1)))
                .andExpect(status().isOk());

        // PAID -> PROCESSING_IN_WAREHOUSE
        com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest updateRequest2 = 
                new com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest();
        updateRequest2.setNewStatus(OrderStatus.PROCESSING_IN_WAREHOUSE);
        updateRequest2.setReason("Processing");
        mockMvc.perform(put("/api/orders/{orderId}/status", orderId)
                        .header("userId", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest2)))
                .andExpect(status().isOk());

        // PROCESSING_IN_WAREHOUSE -> SHIPPED
        com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest updateRequest3 = 
                new com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest();
        updateRequest3.setNewStatus(OrderStatus.SHIPPED);
        updateRequest3.setReason("Shipped");
        mockMvc.perform(put("/api/orders/{orderId}/status", orderId)
                        .header("userId", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest3)))
                .andExpect(status().isOk());

        // SHIPPED -> DELIVERED
        com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest updateRequest4 = 
                new com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest();
        updateRequest4.setNewStatus(OrderStatus.DELIVERED);
        updateRequest4.setReason("Delivered");
        mockMvc.perform(put("/api/orders/{orderId}/status", orderId)
                        .header("userId", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest4)))
                .andExpect(status().isOk());

        // Setup return request
        createReturnRequest = new CreateReturnRequest();
        createReturnRequest.setOrderId(orderId);
        createReturnRequest.setReturnReason("Item damaged during shipping");
    }

    @Test
    void testCreateReturnRequest_API() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.returnReason").value("Item damaged during shipping"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn();

        // Verify return response
        String responseContent = result.getResponse().getContentAsString();
        ReturnResponse returnResponse = objectMapper.readValue(responseContent, ReturnResponse.class);
        assertNotNull(returnResponse.getId());
    }

    @Test
    void testCreateReturnRequest_ValidationError_MissingOrderId() throws Exception {
        // Given
        createReturnRequest.setOrderId(null);

        // When & Then
        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateReturnRequest_ValidationError_MissingReturnReason() throws Exception {
        // Given
        createReturnRequest.setReturnReason(null);

        // When & Then
        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateReturnRequest_OrderNotFound() throws Exception {
        // Given
        createReturnRequest.setOrderId(99999L);

        // When & Then
        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order Not Found"));
    }

    @Test
    void testGetReturnById_API() throws Exception {
        // Given - Create a return first
        MvcResult createResult = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ReturnResponse createdReturn = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ReturnResponse.class);

        // When & Then
        mockMvc.perform(get("/api/returns/{returnId}", createdReturn.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdReturn.getId()))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void testGetReturnById_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/returns/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Return Not Found"));
    }

    @Test
    void testGetReturnsByOrderId_API() throws Exception {
        // Given - Create a return first
        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/api/returns/order/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value(orderId));
    }

    @Test
    void testApproveReturn_API() throws Exception {
        // Given - Create a return first
        MvcResult createResult = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ReturnResponse createdReturn = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ReturnResponse.class);

        ApproveReturnRequest approveRequest = new ApproveReturnRequest();
        approveRequest.setManagerId(200L);
        approveRequest.setComments("Approved by manager");

        // When & Then
        mockMvc.perform(post("/api/returns/{returnId}/approve", createdReturn.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value(200L));
    }

    @Test
    void testRejectReturn_API() throws Exception {
        // Given - Create a return first
        MvcResult createResult = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ReturnResponse createdReturn = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ReturnResponse.class);

        // When & Then
        mockMvc.perform(post("/api/returns/{returnId}/reject", createdReturn.getId())
                        .param("reason", "Invalid return reason")
                        .header("userId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void testMarkReturnInTransit_API() throws Exception {
        // Given - Create and approve a return first
        MvcResult createResult = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ReturnResponse createdReturn = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ReturnResponse.class);

        // Approve return
        ApproveReturnRequest approveRequest = new ApproveReturnRequest();
        approveRequest.setManagerId(200L);
        mockMvc.perform(post("/api/returns/{returnId}/approve", createdReturn.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk());

        // When & Then
        mockMvc.perform(put("/api/returns/{returnId}/in-transit", createdReturn.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    void testMarkReturnReceived_API() throws Exception {
        // Given - Create, approve, and mark in transit
        MvcResult createResult = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ReturnResponse createdReturn = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ReturnResponse.class);

        // Approve
        ApproveReturnRequest approveRequest = new ApproveReturnRequest();
        approveRequest.setManagerId(200L);
        mockMvc.perform(post("/api/returns/{returnId}/approve", createdReturn.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk());

        // Mark in transit
        mockMvc.perform(put("/api/returns/{returnId}/in-transit", createdReturn.getId()))
                .andExpect(status().isOk());

        // When & Then
        mockMvc.perform(put("/api/returns/{returnId}/received", createdReturn.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void testGetReturnStateHistory_API() throws Exception {
        // Given - Create a return first
        MvcResult createResult = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ReturnResponse createdReturn = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), ReturnResponse.class);

        // When & Then
        mockMvc.perform(get("/api/returns/{returnId}/history", createdReturn.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].entityType").value("RETURN"));
    }

    @Test
    void testCreateReturnRequest_OrderNotDelivered() throws Exception {
        // Given - Create a new order in PENDING_PAYMENT state
        CreateOrderRequest newOrderRequest = new CreateOrderRequest();
        newOrderRequest.setCustomerId(200L);
        newOrderRequest.setShippingAddress("456 Other St");
        
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(102L);
        itemRequest.setProductName("Artisan Bowl");
        itemRequest.setQuantity(1);
        itemRequest.setPrice(new BigDecimal("100.00"));
        newOrderRequest.setOrderItems(Arrays.asList(itemRequest));

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        OrderResponse newOrder = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), OrderResponse.class);

        CreateReturnRequest invalidReturnRequest = new CreateReturnRequest();
        invalidReturnRequest.setOrderId(newOrder.getId());
        invalidReturnRequest.setReturnReason("Test");

        // When & Then
        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReturnRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Return Not Allowed"));
    }

}
