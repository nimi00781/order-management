package com.articurated.ordermanagement.integration;

import com.articurated.ordermanagement.model.dto.request.*;
import com.articurated.ordermanagement.model.dto.response.OrderResponse;
import com.articurated.ordermanagement.model.dto.response.RefundResponse;
import com.articurated.ordermanagement.model.dto.response.ReturnResponse;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.RefundStatus;
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
class RefundApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long orderId;
    private Long returnId;

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

        // Create a return and set it to RECEIVED status
        CreateReturnRequest createReturnRequest = new CreateReturnRequest();
        createReturnRequest.setOrderId(orderId);
        createReturnRequest.setReturnReason("Item damaged");

        MvcResult returnResult = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ReturnResponse returnResponse = objectMapper.readValue(
                returnResult.getResponse().getContentAsString(), ReturnResponse.class);
        returnId = returnResponse.getId();

        // Approve return
        ApproveReturnRequest approveRequest = new ApproveReturnRequest();
        approveRequest.setManagerId(200L);
        mockMvc.perform(post("/api/returns/{returnId}/approve", returnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk());

        // Mark in transit
        mockMvc.perform(put("/api/returns/{returnId}/in-transit", returnId))
                .andExpect(status().isOk());

        // Mark received
        mockMvc.perform(put("/api/returns/{returnId}/received", returnId))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateRefund_API() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(post("/api/refunds/return/{returnId}", returnId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.returnId").value(returnId))
                .andExpect(jsonPath("$.amount").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        // Verify refund response
        String responseContent = result.getResponse().getContentAsString();
        RefundResponse refundResponse = objectMapper.readValue(responseContent, RefundResponse.class);
        assertNotNull(refundResponse.getId());
    }

    @Test
    void testCreateRefund_ReturnNotFound() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/refunds/return/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Return Not Found"));
    }

    @Test
    void testGetRefundById_API() throws Exception {
        // Given - Create a refund first
        MvcResult createResult = mockMvc.perform(post("/api/refunds/return/{returnId}", returnId))
                .andExpect(status().isCreated())
                .andReturn();

        RefundResponse createdRefund = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), RefundResponse.class);

        // When & Then
        mockMvc.perform(get("/api/refunds/{refundId}", createdRefund.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdRefund.getId()))
                .andExpect(jsonPath("$.returnId").value(returnId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetRefundById_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/refunds/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Refund Not Found"));
    }

    @Test
    void testGetRefundByReturnId_API() throws Exception {
        // Given - Create a refund first
        MvcResult createResult = mockMvc.perform(post("/api/refunds/return/{returnId}", returnId))
                .andExpect(status().isCreated())
                .andReturn();

        RefundResponse createdRefund = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), RefundResponse.class);

        // When & Then
        mockMvc.perform(get("/api/refunds/return/{returnId}", returnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdRefund.getId()))
                .andExpect(jsonPath("$.returnId").value(returnId));
    }

    @Test
    void testProcessRefund_API() throws Exception {
        // Given - Create a refund first
        MvcResult createResult = mockMvc.perform(post("/api/refunds/return/{returnId}", returnId))
                .andExpect(status().isCreated())
                .andReturn();

        RefundResponse createdRefund = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), RefundResponse.class);

        // When & Then
        mockMvc.perform(post("/api/refunds/{refundId}/process", createdRefund.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").exists());
    }

    @Test
    void testProcessRefund_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/refunds/99999/process"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Refund Not Found"));
    }

    @Test
    void testCreateRefund_ReturnNotInReceivedState() throws Exception {
        // Given - Create a new return in REQUESTED state
        CreateReturnRequest newReturnRequest = new CreateReturnRequest();
        newReturnRequest.setOrderId(orderId);
        newReturnRequest.setReturnReason("Test return");

        MvcResult returnResult = mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReturnRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ReturnResponse newReturn = objectMapper.readValue(
                returnResult.getResponse().getContentAsString(), ReturnResponse.class);

        // When & Then
        mockMvc.perform(post("/api/refunds/return/{returnId}", newReturn.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Rule Violation"));
    }

    @Test
    void testCreateRefund_DuplicateRefund() throws Exception {
        // Given - Create a refund first
        mockMvc.perform(post("/api/refunds/return/{returnId}", returnId))
                .andExpect(status().isCreated());

        // When & Then - Try to create another refund for same return
        mockMvc.perform(post("/api/refunds/return/{returnId}", returnId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Business Rule Violation"));
    }

}
