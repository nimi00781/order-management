package com.articurated.ordermanagement.controller;

import com.articurated.ordermanagement.model.dto.request.ApproveReturnRequest;
import com.articurated.ordermanagement.model.dto.request.CreateReturnRequest;
import com.articurated.ordermanagement.model.dto.response.ReturnResponse;
import com.articurated.ordermanagement.model.dto.response.StateHistoryResponse;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
import com.articurated.ordermanagement.service.ReturnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReturnController.class)
class ReturnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReturnService returnService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateReturnRequest createReturnRequest;
    private ReturnResponse returnResponse;

    @BeforeEach
    void setUp() {
        createReturnRequest = new CreateReturnRequest();
        createReturnRequest.setOrderId(1L);
        createReturnRequest.setReturnReason("Item damaged");

        returnResponse = ReturnResponse.builder()
                .id(1L)
                .orderId(1L)
                .returnReason("Item damaged")
                .status(ReturnStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateReturnRequest_Success() throws Exception {
        // Given
        when(returnService.createReturnRequest(any(CreateReturnRequest.class))).thenReturn(returnResponse);

        // When & Then
        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReturnRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void testCreateReturnRequest_ValidationError() throws Exception {
        // Given
        CreateReturnRequest invalidRequest = new CreateReturnRequest();
        // Missing required fields

        // When & Then
        mockMvc.perform(post("/api/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetReturnById_Success() throws Exception {
        // Given
        when(returnService.getReturnById(1L)).thenReturn(returnResponse);

        // When & Then
        mockMvc.perform(get("/api/returns/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void testGetReturnsByOrderId_Success() throws Exception {
        // Given
        when(returnService.getReturnsByOrderId(1L)).thenReturn(Arrays.asList(returnResponse));

        // When & Then
        mockMvc.perform(get("/api/returns/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(1L));
    }

    @Test
    void testApproveReturn_Success() throws Exception {
        // Given
        ApproveReturnRequest request = new ApproveReturnRequest();
        request.setManagerId(200L);
        request.setComments("Approved");

        returnResponse.setStatus(ReturnStatus.APPROVED);
        when(returnService.approveReturn(eq(1L), any(ApproveReturnRequest.class))).thenReturn(returnResponse);

        // When & Then
        mockMvc.perform(post("/api/returns/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void testRejectReturn_Success() throws Exception {
        // Given
        returnResponse.setStatus(ReturnStatus.REJECTED);
        when(returnService.rejectReturn(eq(1L), eq("Invalid reason"), eq(200L))).thenReturn(returnResponse);

        // When & Then
        mockMvc.perform(post("/api/returns/1/reject")
                        .param("reason", "Invalid reason")
                        .header("userId", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void testMarkReturnInTransit_Success() throws Exception {
        // Given
        returnResponse.setStatus(ReturnStatus.IN_TRANSIT);
        when(returnService.markReturnInTransit(1L)).thenReturn(returnResponse);

        // When & Then
        mockMvc.perform(put("/api/returns/1/in-transit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    void testMarkReturnReceived_Success() throws Exception {
        // Given
        returnResponse.setStatus(ReturnStatus.RECEIVED);
        when(returnService.markReturnReceived(1L)).thenReturn(returnResponse);

        // When & Then
        mockMvc.perform(put("/api/returns/1/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void testGetReturnStateHistory_Success() throws Exception {
        // Given
        StateHistoryResponse historyResponse = StateHistoryResponse.builder()
                .id(1L)
                .entityId(1L)
                .fromState("REQUESTED")
                .toState("APPROVED")
                .triggeredBy("200")
                .timestamp(LocalDateTime.now())
                .build();

        when(returnService.getReturnStateHistory(1L)).thenReturn(Arrays.asList(historyResponse));

        // When & Then
        mockMvc.perform(get("/api/returns/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fromState").value("REQUESTED"))
                .andExpect(jsonPath("$[0].toState").value("APPROVED"));
    }
}
