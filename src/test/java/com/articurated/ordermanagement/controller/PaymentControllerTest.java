package com.articurated.ordermanagement.controller;

import com.articurated.ordermanagement.model.dto.request.ProcessPaymentRequest;
import com.articurated.ordermanagement.model.dto.response.PaymentResponse;
import com.articurated.ordermanagement.model.enums.PaymentStatus;
import com.articurated.ordermanagement.service.PaymentService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProcessPaymentRequest paymentRequest;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        paymentRequest = new ProcessPaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(new BigDecimal("300.00"));

        paymentResponse = PaymentResponse.builder()
                .id(1L)
                .orderId(1L)
                .paymentMethod("CREDIT_CARD")
                .amount(new BigDecimal("300.00"))
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN-123456")
                .processedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testProcessPayment_Success() throws Exception {
        // Given
        when(paymentService.processPayment(any(ProcessPaymentRequest.class))).thenReturn(paymentResponse);

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value("TXN-123456"));
    }

    @Test
    void testProcessPayment_ValidationError() throws Exception {
        // Given
        ProcessPaymentRequest invalidRequest = new ProcessPaymentRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetPaymentById_Success() throws Exception {
        // Given
        when(paymentService.getPaymentById(1L)).thenReturn(paymentResponse);

        // When & Then
        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testGetPaymentsByOrderId_Success() throws Exception {
        // Given
        when(paymentService.getPaymentsByOrderId(1L)).thenReturn(Arrays.asList(paymentResponse));

        // When & Then
        mockMvc.perform(get("/api/payments/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1L));
    }

    @Test
    void testHandlePaymentWebhook_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
