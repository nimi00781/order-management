package com.articurated.ordermanagement.model.dto.request;

import com.articurated.ordermanagement.model.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    @NotNull(message = "New status is required")
    private OrderStatus newStatus;

    private String reason;
}
