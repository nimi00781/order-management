package com.articurated.ordermanagement.model.entity;

import com.articurated.ordermanagement.model.enums.EntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "state_transitions", indexes = {
    @Index(name = "idx_entity_type_id", columnList = "entity_type,entity_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateTransition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "from_state", length = 50)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 50)
    private String toState;

    @Column(name = "triggered_by", nullable = false, length = 100)
    private String triggeredBy;

    @Column(name = "transition_reason", length = 500)
    private String transitionReason;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
