package com.articurated.ordermanagement.model.dto.response;

import com.articurated.ordermanagement.model.enums.ReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnResponse {
    private Long id;
    private Long orderId;
    private String returnReason;
    private ReturnStatus status;
    private LocalDateTime requestedAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
    private String comments;
}
