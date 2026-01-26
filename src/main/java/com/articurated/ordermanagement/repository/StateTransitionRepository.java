package com.articurated.ordermanagement.repository;

import com.articurated.ordermanagement.model.entity.StateTransition;
import com.articurated.ordermanagement.model.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StateTransitionRepository extends JpaRepository<StateTransition, Long> {
    List<StateTransition> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);
    List<StateTransition> findByEntityTypeAndEntityIdOrderByTimestampDesc(EntityType entityType, Long entityId);
    List<StateTransition> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
