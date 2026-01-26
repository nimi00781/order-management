package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.integration.PaymentGatewayClient;
import com.articurated.ordermanagement.model.dto.response.RefundResponse;
import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.Refund;
import com.articurated.ordermanagement.model.entity.Return;
import com.articurated.ordermanagement.model.enums.RefundStatus;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
import com.articurated.ordermanagement.model.exception.BusinessRuleViolationException;
import com.articurated.ordermanagement.model.exception.PaymentProcessingException;
import com.articurated.ordermanagement.model.exception.RefundNotFoundException;
import com.articurated.ordermanagement.model.exception.ReturnNotFoundException;
import com.articurated.ordermanagement.repository.RefundRepository;
import com.articurated.ordermanagement.repository.ReturnRepository;
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
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private ReturnRepository returnRepository;

    @Mock
    private PaymentGatewayClient paymentGatewayClient;

    @Mock
    private StateMachineService stateMachineService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RefundService refundService;

    private Order testOrder;
    private Return testReturn;
    private Refund testRefund;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-2024-ABC123")
                .totalAmount(new BigDecimal("300.00"))
                .build();

        testReturn = Return.builder()
                .id(1L)
                .order(testOrder)
                .status(ReturnStatus.RECEIVED)
                .build();

        testRefund = Refund.builder()
                .id(1L)
                .returnEntity(testReturn)
                .amount(new BigDecimal("300.00"))
                .status(RefundStatus.PENDING)
                .refundMethod("ORIGINAL_PAYMENT_METHOD")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateRefund_Success() {
        // Given
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund refund = invocation.getArgument(0);
            refund.setId(1L);
            return refund;
        });
        when(returnRepository.save(any(Return.class))).thenReturn(testReturn);

        // When
        RefundResponse response = refundService.createRefund(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getReturnId());
        assertEquals(RefundStatus.PENDING, response.getStatus());
        verify(returnRepository, times(1)).findById(1L);
        verify(refundRepository, times(1)).save(any(Refund.class));
    }

    @Test
    void testCreateRefund_ReturnNotFound() {
        // Given
        when(returnRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ReturnNotFoundException.class, () -> refundService.createRefund(999L));
        verify(returnRepository, times(1)).findById(999L);
        verify(refundRepository, never()).save(any());
    }

    @Test
    void testCreateRefund_ReturnNotInReceivedState() {
        // Given
        testReturn.setStatus(ReturnStatus.APPROVED);
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        // When & Then
        assertThrows(BusinessRuleViolationException.class, () -> refundService.createRefund(1L));
        verify(returnRepository, times(1)).findById(1L);
        verify(refundRepository, never()).save(any());
    }

    @Test
    void testCreateRefund_RefundAlreadyExists() {
        // Given
        testReturn.setRefund(testRefund);
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        // When & Then
        assertThrows(BusinessRuleViolationException.class, () -> refundService.createRefund(1L));
        verify(returnRepository, times(1)).findById(1L);
        verify(refundRepository, never()).save(any());
    }

    @Test
    void testGetRefundById_Success() {
        // Given
        when(refundRepository.findById(1L)).thenReturn(Optional.of(testRefund));

        // When
        RefundResponse response = refundService.getRefundById(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(RefundStatus.PENDING, response.getStatus());
        verify(refundRepository, times(1)).findById(1L);
    }

    @Test
    void testGetRefundById_NotFound() {
        // Given
        when(refundRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RefundNotFoundException.class, () -> refundService.getRefundById(999L));
        verify(refundRepository, times(1)).findById(999L);
    }

    @Test
    void testGetRefundByReturnId_Success() {
        // Given
        when(refundRepository.findByReturnId(1L)).thenReturn(Optional.of(testRefund));

        // When
        RefundResponse response = refundService.getRefundByReturnId(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getReturnId());
        verify(refundRepository, times(1)).findByReturnId(1L);
    }

    @Test
    void testProcessRefund_Success() {
        // Given
        when(refundRepository.findById(1L)).thenReturn(Optional.of(testRefund));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund refund = invocation.getArgument(0);
            refund.setTransactionId("REF-123456");
            refund.setStatus(RefundStatus.COMPLETED);
            refund.setProcessedAt(LocalDateTime.now());
            return refund;
        });

        PaymentGatewayClient.RefundResponse gatewayResponse = 
                PaymentGatewayClient.RefundResponse.builder()
                        .success(true)
                        .transactionId("REF-123456")
                        .message("Refund processed successfully")
                        .build();

        when(paymentGatewayClient.processRefund(any())).thenReturn(gatewayResponse);
        when(returnRepository.save(any(Return.class))).thenReturn(testReturn);

        // When
        RefundResponse response = refundService.processRefund(1L);

        // Then
        assertNotNull(response);
        assertEquals(RefundStatus.COMPLETED, response.getStatus());
        assertEquals("REF-123456", response.getTransactionId());
        verify(refundRepository, times(2)).save(any(Refund.class));
        verify(paymentGatewayClient, times(1)).processRefund(any());
        verify(stateMachineService, times(1)).transitionReturnState(
                any(Return.class), eq(ReturnStatus.COMPLETED), isNull(), anyString());
        verify(emailService, times(1)).sendRefundConfirmation(any(Refund.class));
    }

    @Test
    void testProcessRefund_NotFound() {
        // Given
        when(refundRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RefundNotFoundException.class, () -> refundService.processRefund(999L));
        verify(refundRepository, times(1)).findById(999L);
    }

    @Test
    void testProcessRefund_NotInPendingState() {
        // Given
        testRefund.setStatus(RefundStatus.PROCESSING);
        when(refundRepository.findById(1L)).thenReturn(Optional.of(testRefund));

        // When & Then
        assertThrows(BusinessRuleViolationException.class, () -> refundService.processRefund(1L));
        verify(refundRepository, times(1)).findById(1L);
        verify(paymentGatewayClient, never()).processRefund(any());
    }

    @Test
    void testProcessRefund_GatewayFailure() {
        // Given
        when(refundRepository.findById(1L)).thenReturn(Optional.of(testRefund));
        when(refundRepository.save(any(Refund.class))).thenReturn(testRefund);

        PaymentGatewayClient.RefundResponse gatewayResponse = 
                PaymentGatewayClient.RefundResponse.builder()
                        .success(false)
                        .message("Refund failed")
                        .build();

        when(paymentGatewayClient.processRefund(any())).thenReturn(gatewayResponse);

        // When
        RefundResponse response = refundService.processRefund(1L);

        // Then
        assertNotNull(response);
        assertEquals(RefundStatus.FAILED, response.getStatus());
        verify(refundRepository, times(2)).save(any(Refund.class));
        verify(stateMachineService, never()).transitionReturnState(any(), any(), any(), any());
    }

    @Test
    void testProcessRefund_GatewayException() {
        // Given
        when(refundRepository.findById(1L)).thenReturn(Optional.of(testRefund));
        when(refundRepository.save(any(Refund.class))).thenReturn(testRefund);
        when(paymentGatewayClient.processRefund(any())).thenThrow(new RuntimeException("Gateway error"));

        // When & Then
        assertThrows(PaymentProcessingException.class, () -> refundService.processRefund(1L));
        verify(refundRepository, times(2)).save(any(Refund.class));
    }

    @Test
    void testGetPendingRefunds_Success() {
        // Given
        when(refundRepository.findByStatus(RefundStatus.PENDING)).thenReturn(Arrays.asList(testRefund));

        // When
        List<RefundResponse> responses = refundService.getPendingRefunds();

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(RefundStatus.PENDING, responses.get(0).getStatus());
        verify(refundRepository, times(1)).findByStatus(RefundStatus.PENDING);
    }
}
