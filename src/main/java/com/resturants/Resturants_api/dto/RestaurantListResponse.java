package com.resturants.Resturants_api.dto;

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
