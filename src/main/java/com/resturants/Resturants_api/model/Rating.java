package com.resturants.Resturants_api.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class Rating {

    @DecimalMin(value = "0.0", message = "Aggregate rating must be between 0.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Aggregate rating must be between 0.0 and 5.0")
    private double aggregateRating;

    @Size(max = 50, message = "Rating color must not exceed 50 characters")
    private String ratingColor;

    @Size(max = 50, message = "Rating text must not exceed 50 characters")
    private String ratingText;

    @PositiveOrZero(message = "Votes must be non-negative")
    private int votes;
}
