package com.resturants.Resturants_api.repository;

import com.resturants.Resturants_api.model.Restaurant;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RestaurantRepository extends MongoRepository<Restaurant, Long> {
}
