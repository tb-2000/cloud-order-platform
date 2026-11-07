package com.cloudorder.service.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
    
    @NotBlank 
    @Size (max = 150)
    String name,

    String description,

    @NotNull 
    @DecimalMin ("0.00")
    @Digits (integer = 10, fraction = 2)
    BigDecimal price,

    @NotNull
    @Min (0)
    Integer stock
) {}
