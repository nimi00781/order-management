package com.articurated.ordermanagement.model.dto.response;

import com.articurated.ordermanagement.model.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private Long id;
    private Long returnId;
    private BigDecimal amount;
    private RefundStatus status;
    private String refundMethod;
    private String transactionId;
    private LocalDateTime processedAt;
}
