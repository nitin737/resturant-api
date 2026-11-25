package com.resturants.Resturants_api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Document(collection = "restaurants")
@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Restaurant {

        @Id
        @Positive(message = "Restaurant ID must be positive")
        private Long restaurantId;

        @NotBlank(message = "Restaurant name must not be blank")
        @Size(max = 200, message = "Restaurant name must not exceed 200 characters")
        private String restaurantName;

        @Size(max = 255, message = "Cuisines must not exceed 255 characters")
        private String cuisines;

        @Valid
        private Location location;

        @Valid
        private Pricing pricing;

        @Valid
        private Features features;

        @Valid
        private Rating rating;
}
