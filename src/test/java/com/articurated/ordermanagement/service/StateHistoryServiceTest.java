package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.model.entity.StateTransition;
import com.articurated.ordermanagement.model.enums.EntityType;
import com.articurated.ordermanagement.repository.StateTransitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateHistoryServiceTest {

    @Mock
    private StateTransitionRepository stateTransitionRepository;

    @InjectMocks
    private StateHistoryService stateHistoryService;

    private StateTransition testTransition;

    @BeforeEach
    void setUp() {
        testTransition = StateTransition.builder()
                .id(1L)
                .entityType(EntityType.ORDER)
                .entityId(1L)
                .fromState("PENDING_PAYMENT")
                .toState("PAID")
                .triggeredBy("100")
                .transitionReason("Payment received")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void testLogStateTransition_Success() {
        // Given
        when(stateTransitionRepository.save(any(StateTransition.class))).thenReturn(testTransition);

        // When
        StateTransition result = stateHistoryService.logStateTransition(
                EntityType.ORDER, 1L, "PENDING_PAYMENT", "PAID", 100L, "Payment received");

        // Then
        assertNotNull(result);
        assertEquals(EntityType.ORDER, result.getEntityType());
        assertEquals(1L, result.getEntityId());
        verify(stateTransitionRepository, times(1)).save(any(StateTransition.class));
    }

    @Test
    void testLogStateTransition_WithSystemUser() {
        // Given
        when(stateTransitionRepository.save(any(StateTransition.class))).thenReturn(testTransition);

        // When
        StateTransition result = stateHistoryService.logStateTransition(
                EntityType.ORDER, 1L, "PENDING_PAYMENT", "PAID", null, "System transition");

        // Then
        assertNotNull(result);
        verify(stateTransitionRepository, times(1)).save(any(StateTransition.class));
    }

    @Test
    void testGetStateHistory_Success() {
        // Given
        when(stateTransitionRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                EntityType.ORDER, 1L)).thenReturn(Arrays.asList(testTransition));

        // When
        List<StateTransition> history = stateHistoryService.getStateHistory(EntityType.ORDER, 1L);

        // Then
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(EntityType.ORDER, history.get(0).getEntityType());
        verify(stateTransitionRepository, times(1))
                .findByEntityTypeAndEntityIdOrderByTimestampDesc(EntityType.ORDER, 1L);
    }

    @Test
    void testGetStateHistoryByDateRange_Success() {
        // Given
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        
        when(stateTransitionRepository.findByTimestampBetween(start, end))
                .thenReturn(Arrays.asList(testTransition));

        // When
        List<StateTransition> history = stateHistoryService.getStateHistoryByDateRange(
                EntityType.ORDER, 1L, start, end);

        // Then
        assertNotNull(history);
        assertEquals(1, history.size());
        verify(stateTransitionRepository, times(1)).findByTimestampBetween(start, end);
    }

    @Test
    void testGetLatestStateTransition_Success() {
        // Given
        when(stateTransitionRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                EntityType.ORDER, 1L)).thenReturn(Arrays.asList(testTransition));

        // When
        Optional<StateTransition> result = stateHistoryService.getLatestStateTransition(EntityType.ORDER, 1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testTransition, result.get());
    }

    @Test
    void testGetLatestStateTransition_Empty() {
        // Given
        when(stateTransitionRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(
                EntityType.ORDER, 1L)).thenReturn(Arrays.asList());

        // When
        Optional<StateTransition> result = stateHistoryService.getLatestStateTransition(EntityType.ORDER, 1L);

        // Then
        assertFalse(result.isPresent());
    }
}
