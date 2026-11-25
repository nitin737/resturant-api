package com.resturants.Resturants_api.exception;

public class RestaurantNotFoundException extends RuntimeException {

    public RestaurantNotFoundException(long id) {
        super("Restaurant not found with id: " + id);
    }
}
