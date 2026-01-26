package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.model.dto.request.ApproveReturnRequest;
import com.articurated.ordermanagement.model.dto.request.CreateReturnRequest;
import com.articurated.ordermanagement.model.dto.response.ReturnResponse;
import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.Return;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
import com.articurated.ordermanagement.model.exception.BusinessRuleViolationException;
import com.articurated.ordermanagement.model.exception.OrderNotFoundException;
import com.articurated.ordermanagement.model.exception.ReturnNotAllowedException;
import com.articurated.ordermanagement.model.exception.ReturnNotFoundException;
import com.articurated.ordermanagement.repository.OrderRepository;
import com.articurated.ordermanagement.repository.ReturnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private ReturnRepository returnRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StateMachineService stateMachineService;

    @Mock
    private StateHistoryService stateHistoryService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReturnService returnService;

    private Order testOrder;
    private Return testReturn;
    private CreateReturnRequest createReturnRequest;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-2024-ABC123")
                .customerId(100L)
                .status(OrderStatus.DELIVERED)
                .build();

        testReturn = Return.builder()
                .id(1L)
                .order(testOrder)
                .returnReason("Item damaged")
                .status(ReturnStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        createReturnRequest = new CreateReturnRequest();
        createReturnRequest.setOrderId(1L);
        createReturnRequest.setReturnReason("Item damaged");
    }

    @Test
    void testCreateReturnRequest_Success() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(returnRepository.findByOrderIdAndStatus(1L, ReturnStatus.REQUESTED))
                .thenReturn(Collections.emptyList());
        when(returnRepository.save(any(Return.class))).thenAnswer(invocation -> {
            Return returnEntity = invocation.getArgument(0);
            returnEntity.setId(1L);
            return returnEntity;
        });

        // When
        ReturnResponse response = returnService.createReturnRequest(createReturnRequest);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getOrderId());
        assertEquals("Item damaged", response.getReturnReason());
        assertEquals(ReturnStatus.REQUESTED, response.getStatus());
        verify(orderRepository, times(1)).findById(1L);
        verify(returnRepository, times(1)).save(any(Return.class));
        verify(stateHistoryService, times(1)).logStateTransition(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testCreateReturnRequest_OrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        createReturnRequest.setOrderId(999L);

        // When & Then
        assertThrows(OrderNotFoundException.class, () -> returnService.createReturnRequest(createReturnRequest));
        verify(orderRepository, times(1)).findById(999L);
        verify(returnRepository, never()).save(any());
    }

    @Test
    void testCreateReturnRequest_OrderNotDelivered() {
        // Given
        testOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(ReturnNotAllowedException.class, () -> returnService.createReturnRequest(createReturnRequest));
        verify(orderRepository, times(1)).findById(1L);
        verify(returnRepository, never()).save(any());
    }

    @Test
    void testCreateReturnRequest_ActiveReturnExists() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(returnRepository.findByOrderIdAndStatus(1L, ReturnStatus.REQUESTED))
                .thenReturn(Arrays.asList(testReturn));

        // When & Then
        assertThrows(ReturnNotAllowedException.class, () -> returnService.createReturnRequest(createReturnRequest));
        verify(orderRepository, times(1)).findById(1L);
        verify(returnRepository, never()).save(any());
    }

    @Test
    void testGetReturnById_Success() {
        // Given
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        // When
        ReturnResponse response = returnService.getReturnById(1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(ReturnStatus.REQUESTED, response.getStatus());
        verify(returnRepository, times(1)).findById(1L);
    }

    @Test
    void testGetReturnById_NotFound() {
        // Given
        when(returnRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ReturnNotFoundException.class, () -> returnService.getReturnById(999L));
        verify(returnRepository, times(1)).findById(999L);
    }

    @Test
    void testApproveReturn_Success() {
        // Given
        ApproveReturnRequest request = new ApproveReturnRequest();
        request.setManagerId(200L);
        request.setComments("Approved by manager");

        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));
        when(stateMachineService.transitionReturnState(any(), any(), any(), any())).thenReturn(testReturn);
        when(returnRepository.save(any(Return.class))).thenReturn(testReturn);

        // When
        ReturnResponse response = returnService.approveReturn(1L, request);

        // Then
        assertNotNull(response);
        verify(stateMachineService, times(1)).transitionReturnState(
                any(Return.class), eq(ReturnStatus.APPROVED), eq(200L), anyString());
        verify(returnRepository, times(1)).save(any(Return.class));
        verify(emailService, times(1)).sendReturnStatusUpdate(any(), eq(ReturnStatus.APPROVED));
    }

    @Test
    void testApproveReturn_NotFound() {
        // Given
        ApproveReturnRequest request = new ApproveReturnRequest();
        when(returnRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ReturnNotFoundException.class, () -> returnService.approveReturn(999L, request));
        verify(returnRepository, times(1)).findById(999L);
    }

    @Test
    void testApproveReturn_CannotBeApproved() {
        // Given
        testReturn.setStatus(ReturnStatus.APPROVED);
        ApproveReturnRequest request = new ApproveReturnRequest();
        request.setManagerId(200L);

        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        // When & Then
        assertThrows(BusinessRuleViolationException.class, () -> returnService.approveReturn(1L, request));
        verify(returnRepository, times(1)).findById(1L);
        verify(stateMachineService, never()).transitionReturnState(any(), any(), any(), any());
    }

    @Test
    void testRejectReturn_Success() {
        // Given
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));
        when(stateMachineService.transitionReturnState(any(), any(), any(), any())).thenReturn(testReturn);
        when(returnRepository.save(any(Return.class))).thenReturn(testReturn);

        // When
        ReturnResponse response = returnService.rejectReturn(1L, "Invalid reason", 200L);

        // Then
        assertNotNull(response);
        verify(stateMachineService, times(1)).transitionReturnState(
                any(Return.class), eq(ReturnStatus.REJECTED), eq(200L), anyString());
        verify(returnRepository, times(1)).save(any(Return.class));
        verify(emailService, times(1)).sendReturnStatusUpdate(any(), eq(ReturnStatus.REJECTED));
    }

    @Test
    void testRejectReturn_NotInRequestedState() {
        // Given
        testReturn.setStatus(ReturnStatus.APPROVED);
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));

        // When & Then
        assertThrows(BusinessRuleViolationException.class, 
                () -> returnService.rejectReturn(1L, "Reason", 200L));
        verify(returnRepository, times(1)).findById(1L);
    }

    @Test
    void testMarkReturnInTransit_Success() {
        // Given
        testReturn.setStatus(ReturnStatus.APPROVED);
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));
        when(stateMachineService.transitionReturnState(any(), any(), any(), any())).thenReturn(testReturn);
        when(returnRepository.save(any(Return.class))).thenReturn(testReturn);

        // When
        ReturnResponse response = returnService.markReturnInTransit(1L);

        // Then
        assertNotNull(response);
        verify(stateMachineService, times(1)).transitionReturnState(
                any(Return.class), eq(ReturnStatus.IN_TRANSIT), isNull(), anyString());
        verify(returnRepository, times(1)).save(any(Return.class));
    }

    @Test
    void testMarkReturnReceived_Success() {
        // Given
        testReturn.setStatus(ReturnStatus.IN_TRANSIT);
        when(returnRepository.findById(1L)).thenReturn(Optional.of(testReturn));
        when(stateMachineService.transitionReturnState(any(), any(), any(), any())).thenReturn(testReturn);
        when(returnRepository.save(any(Return.class))).thenReturn(testReturn);

        // When
        ReturnResponse response = returnService.markReturnReceived(1L);

        // Then
        assertNotNull(response);
        verify(stateMachineService, times(1)).transitionReturnState(
                any(Return.class), eq(ReturnStatus.RECEIVED), isNull(), anyString());
        verify(returnRepository, times(1)).save(any(Return.class));
    }

    @Test
    void testGetReturnsByOrderId_Success() {
        // Given
        when(returnRepository.findByOrderId(1L)).thenReturn(Arrays.asList(testReturn));

        // When
        List<ReturnResponse> responses = returnService.getReturnsByOrderId(1L);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        verify(returnRepository, times(1)).findByOrderId(1L);
    }
}
