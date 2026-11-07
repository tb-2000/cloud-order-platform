package com.cloudorder.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockUpdateRequest(
    
    @NotNull(message = "Stock value cannot be null")
    @Min(value = 0, message = "Stock value must be greater than or equal to 0")
    Integer stock
) {
}