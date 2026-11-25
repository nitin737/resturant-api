package com.resturants.repository;

import com.resturants.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RestaurantRepository extends MongoRepository<Restaurant, Long> {
}
