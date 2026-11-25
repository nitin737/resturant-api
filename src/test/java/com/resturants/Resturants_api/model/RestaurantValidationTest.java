package com.resturants.Resturants_api.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestaurantValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private Restaurant validRestaurant() {
        return Restaurant.builder()
                .restaurantId(1L)
                .restaurantName("The Golden Fork")
                .cuisines("Italian, Mediterranean")
                .location(new Location(1, "India", "Bengaluru", "123 MG Road",
                        "MG Road", "MG Road, Bengaluru", 77.5946, 12.9716))
                .pricing(new Pricing(1200, "INR", 3))
                .features(new Features(true, true, false, false))
                .rating(new Rating(4.5, "Dark Green", "Excellent", 250))
                .build();
    }

    @Test
    void validRestaurantHasNoViolations() {
        Set<ConstraintViolation<Restaurant>> violations = validator.validate(validRestaurant());
        assertTrue(violations.isEmpty());
    }

    @Test
    void rejectsNonPositiveRestaurantIdAndBlankName() {
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(0L)
                .restaurantName("   ")
                .build();

        Set<ConstraintViolation<Restaurant>> violations = validator.validate(restaurant);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("restaurantId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("restaurantName")));
    }

    @Test
    void rejectsInvalidNestedLocationFields() {
        Restaurant restaurant = validRestaurant();
        restaurant.setLocation(new Location(0, "", "", "", null, null, 200.0, -95.0));

        Set<ConstraintViolation<Restaurant>> violations = validator.validate(restaurant);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("location.countryCode")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("location.country")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("location.city")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("location.address")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("location.longitude")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("location.latitude")));
    }

    @Test
    void rejectsInvalidNestedPricingFields() {
        Restaurant restaurant = validRestaurant();
        restaurant.setPricing(new Pricing(-100, " ", 5));

        Set<ConstraintViolation<Restaurant>> violations = validator.validate(restaurant);
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("pricing.averageCostForTwo")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("pricing.currency")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("pricing.priceRange")));
    }

    @Test
    void rejectsInvalidNestedRatingFields() {
        Restaurant restaurant = validRestaurant();
        restaurant.setRating(new Rating(5.5, "Green", "Good", -1));

        Set<ConstraintViolation<Restaurant>> violations = validator.validate(restaurant);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rating.aggregateRating")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("rating.votes")));
    }
}
