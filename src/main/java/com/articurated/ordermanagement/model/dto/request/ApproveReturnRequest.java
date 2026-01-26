package com.articurated.ordermanagement.model.dto.request;

import lombok.Data;

@Data
public class ApproveReturnRequest {
    private String comments;
    @jakarta.validation.constraints.NotNull(message = "Manager ID is required")
    private Long managerId;
}
