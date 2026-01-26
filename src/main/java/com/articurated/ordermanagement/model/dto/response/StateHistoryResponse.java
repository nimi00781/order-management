package com.articurated.ordermanagement.model.dto.response;

import com.articurated.ordermanagement.model.enums.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StateHistoryResponse {
    private Long id;
    private EntityType entityType;
    private Long entityId;
    private String fromState;
    private String toState;
    private String triggeredBy;
    private String transitionReason;
    private LocalDateTime timestamp;
}
