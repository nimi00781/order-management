package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.model.entity.StateTransition;
import com.articurated.ordermanagement.model.enums.EntityType;
import com.articurated.ordermanagement.repository.StateTransitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StateHistoryService {
    private final StateTransitionRepository stateTransitionRepository;

    @Transactional
    public StateTransition logStateTransition(EntityType type, Long entityId, String fromState, 
                                               String toState, Long userId, String reason) {
        StateTransition transition = StateTransition.builder()
                .entityType(type)
                .entityId(entityId)
                .fromState(fromState)
                .toState(toState)
                .triggeredBy(userId != null ? String.valueOf(userId) : "SYSTEM")
                .transitionReason(reason)
                .build();
        return stateTransitionRepository.save(transition);
    }

    public List<StateTransition> getStateHistory(EntityType type, Long entityId) {
        return stateTransitionRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(type, entityId);
    }

    public List<StateTransition> getStateHistoryByDateRange(EntityType type, Long entityId, 
                                                             LocalDateTime start, LocalDateTime end) {
        return stateTransitionRepository.findByTimestampBetween(start, end).stream()
                .filter(t -> t.getEntityType() == type && t.getEntityId().equals(entityId))
                .toList();
    }

    public Optional<StateTransition> getLatestStateTransition(EntityType type, Long entityId) {
        List<StateTransition> transitions = getStateHistory(type, entityId);
        return transitions.isEmpty() ? Optional.empty() : Optional.of(transitions.get(0));
    }
}
