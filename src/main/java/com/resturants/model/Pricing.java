package com.resturants.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pricing {

    @PositiveOrZero(message = "Average cost for two must be non-negative")
    private int averageCostForTwo;

    @NotBlank(message = "Currency must not be blank")
    @Size(max = 50, message = "Currency must not exceed 50 characters")
    private String currency;

    @Min(value = 1, message = "Price range must be between 1 and 4")
    @Max(value = 4, message = "Price range must be between 1 and 4")
    private int priceRange;
}
