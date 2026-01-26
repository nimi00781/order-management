package com.articurated.ordermanagement.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReturnRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Return reason is required")
    private String returnReason;
}
