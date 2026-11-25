package com.resturants.dto;

public record RestaurantListResponse(
                long id,
                String name,
                String address,
                String city,
                String country,
                String cuisines,
                double averageRating,
                Double distance) {
}
