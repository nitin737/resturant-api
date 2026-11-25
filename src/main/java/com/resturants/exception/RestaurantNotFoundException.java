package com.resturants.exception;

public class RestaurantNotFoundException extends RuntimeException {

    public RestaurantNotFoundException(long id) {
        super("Restaurant not found with id: " + id);
    }
}
