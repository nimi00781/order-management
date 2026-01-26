package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.integration.PaymentGatewayClient;
import com.articurated.ordermanagement.model.dto.request.ProcessPaymentRequest;
import com.articurated.ordermanagement.model.dto.response.PaymentResponse;
import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.Payment;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.PaymentStatus;
import com.articurated.ordermanagement.model.exception.BusinessRuleViolationException;
import com.articurated.ordermanagement.model.exception.OrderNotFoundException;
import com.articurated.ordermanagement.model.exception.PaymentNotFoundException;
import com.articurated.ordermanagement.model.exception.PaymentProcessingException;
import com.articurated.ordermanagement.repository.OrderRepository;
import com.articurated.ordermanagement.repository.PaymentRepository;
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
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentGatewayClient paymentGatewayClient;

    @Mock
    private StateMachineService stateMachineService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentService paymentService;

    private Order testOrder;
    private Payment testPayment;
    private ProcessPaymentRequest paymentRequest;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-2024-ABC123")
                .customerId(100L)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("300.00"))
                .shippingAddress("123 Main St")
                .build();

        testPayment = Payment.builder()
                .id(1L)
                .order(testOrder)
                .paymentMethod("CREDIT_CARD")
                .amount(new BigDecimal("300.00"))
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN-123456")
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        paymentRequest = new ProcessPaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setPaymentMethod("CREDIT_CARD");
        paymentRequest.setAmount(new BigDecimal("300.00"));
    }

    @Test
    void testProcessPayment_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            payment.setTransactionId("TXN-123456");
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setProcessedAt(LocalDateTime.now());
            return payment;
        });

        PaymentGatewayClient.PaymentGatewayResponse gatewayResponse = 
                PaymentGatewayClient.PaymentGatewayResponse.builder()
                        .success(true)
                        .transactionId("TXN-123456")
                        .message("Payment processed successfully")
                        .build();

        when(paymentGatewayClient.processPayment(any())).thenReturn(gatewayResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        PaymentResponse response = paymentService.processPayment(paymentRequest);

        // Then
        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("TXN-123456", response.getTransactionId());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(paymentGatewayClient, times(1)).processPayment(any());
        verify(stateMachineService, times(1)).transitionOrderState(
                any(Order.class), eq(OrderStatus.PAID), isNull(), anyString());
        verify(emailService, times(1)).sendOrderConfirmation(any(Order.class));
    }

    @Test
    void testProcessPayment_OrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        paymentRequest.setOrderId(999L);

        // When & Then
        assertThrows(OrderNotFoundException.class, () -> paymentService.processPayment(paymentRequest));
        verify(orderRepository, times(1)).findById(999L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testProcessPayment_OrderNotInPendingPaymentState() {
        // Given
        testOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(BusinessRuleViolationException.class, () -> paymentService.processPayment(paymentRequest));
        verify(orderRepository, times(1)).findById(1L);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void testProcessPayment_GatewayFailure() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });

        PaymentGatewayClient.PaymentGatewayResponse gatewayResponse = 
                PaymentGatewayClient.PaymentGatewayResponse.builder()
                        .success(false)
                        .message("Payment failed")
                        .build();

        when(paymentGatewayClient.processPayment(any())).thenReturn(gatewayResponse);

        // When
        PaymentResponse response = paymentService.processPayment(paymentRequest);

        // Then
        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(stateMachineService, never()).transitionOrderState(any(), any(), any(), any());
    }

    @Test
    void testProcessPayment_GatewayException() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            return payment;
        });
        when(paymentGatewayClient.processPayment(any())).thenThrow(new RuntimeException("Gateway error"));

        // When & Then
        assertThrows(PaymentProcessingException.class, () -> paymentService.processPayment(paymentRequest));
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void testGetPaymentById_Success() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        // When
        PaymentResponse response = paymentService.getPaymentById(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(paymentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetPaymentById_NotFound() {
        // Given
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(999L));
        verify(paymentRepository, times(1)).findById(999L);
    }

    @Test
    void testGetPaymentsByOrderId_Success() {
        // Given
        when(paymentRepository.findByOrderId(1L)).thenReturn(Arrays.asList(testPayment));

        // When
        List<PaymentResponse> responses = paymentService.getPaymentsByOrderId(1L);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        verify(paymentRepository, times(1)).findByOrderId(1L);
    }

    @Test
    void testCheckPaymentStatus_Success() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));

        // When
        PaymentStatus status = paymentService.checkPaymentStatus(1L);

        // Then
        assertEquals(PaymentStatus.SUCCESS, status);
        verify(paymentRepository, times(1)).findById(1L);
    }
}
