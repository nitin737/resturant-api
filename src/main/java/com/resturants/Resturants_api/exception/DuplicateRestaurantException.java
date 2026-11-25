package com.resturants.Resturants_api.exception;

public class DuplicateRestaurantException extends RuntimeException {

    public DuplicateRestaurantException(long id) {
        super("Restaurant already exists with id: " + id);
    }
}
