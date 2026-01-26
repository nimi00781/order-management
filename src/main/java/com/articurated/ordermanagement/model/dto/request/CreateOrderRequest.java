package com.articurated.ordermanagement.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotEmpty(message = "At least one order item is required")
    @Valid
    private List<OrderItemRequest> orderItems;

    @NotNull(message = "Shipping address is required")
    private String shippingAddress;
}
