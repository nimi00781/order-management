package com.articurated.ordermanagement.controller;

import com.articurated.ordermanagement.model.dto.response.RefundResponse;
import com.articurated.ordermanagement.model.enums.RefundStatus;
import com.articurated.ordermanagement.service.RefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RefundController.class)
class RefundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RefundService refundService;

    private RefundResponse refundResponse;

    @BeforeEach
    void setUp() {
        refundResponse = RefundResponse.builder()
                .id(1L)
                .returnId(1L)
                .amount(new BigDecimal("300.00"))
                .status(RefundStatus.PENDING)
                .refundMethod("ORIGINAL_PAYMENT_METHOD")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateRefund_Success() throws Exception {
        // Given
        when(refundService.createRefund(1L)).thenReturn(refundResponse);

        // When & Then
        mockMvc.perform(post("/api/refunds/return/1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetRefundById_Success() throws Exception {
        // Given
        when(refundService.getRefundById(1L)).thenReturn(refundResponse);

        // When & Then
        mockMvc.perform(get("/api/refunds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetRefundByReturnId_Success() throws Exception {
        // Given
        when(refundService.getRefundByReturnId(1L)).thenReturn(refundResponse);

        // When & Then
        mockMvc.perform(get("/api/refunds/return/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnId").value(1L));
    }

    @Test
    void testProcessRefund_Success() throws Exception {
        // Given
        refundResponse.setStatus(RefundStatus.COMPLETED);
        refundResponse.setTransactionId("REF-123456");
        when(refundService.processRefund(1L)).thenReturn(refundResponse);

        // When & Then
        mockMvc.perform(post("/api/refunds/1/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").value("REF-123456"));
    }
}
