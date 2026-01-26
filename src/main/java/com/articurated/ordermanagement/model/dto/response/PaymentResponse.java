package com.articurated.ordermanagement.model.dto.response;

import com.articurated.ordermanagement.model.enums.PaymentStatus;
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
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String paymentMethod;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime processedAt;
}
