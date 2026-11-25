package com.resturants.Resturants_api.controller;

import com.resturants.Resturants_api.AbstractIntegrationTest;
import com.resturants.Resturants_api.model.Location;
import com.resturants.Resturants_api.model.Rating;
import com.resturants.Resturants_api.model.Restaurant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestaurantControllerIT extends AbstractIntegrationTest {

    private Restaurant buildRestaurant(long id, String name, String city, String cuisines,
                                       double rating, double lat, double lon) {
        return Restaurant.builder()
                .restaurantId(id)
                .restaurantName(name)
                .cuisines(cuisines)
                .location(Location.builder()
                        .countryCode(1)
                        .country("India")
                        .city(city)
                        .address("123 Main St, " + city)
                        .latitude(lat)
                        .longitude(lon)
                        .build())
                .rating(Rating.builder()
                        .aggregateRating(rating)
                        .ratingColor("Dark Green")
                        .ratingText("Excellent")
                        .votes(100)
                        .build())
                .build();
    }

    private void seedSearchDataset() {
        restaurantRepository.saveAll(List.of(
                buildRestaurant(1L, "Alpha Bistro", "Delhi", "Italian, Cafe", 4.5, 28.6139, 77.2090),
                buildRestaurant(2L, "Beta Spice", "Delhi", "North Indian, Mughlai", 4.2, 28.5355, 77.2100),
                buildRestaurant(3L, "Gamma Sushi", "Mumbai", "Japanese, Asian", 4.8, 19.0760, 72.8777),
                buildRestaurant(4L, "Delta Tacos", "Bengaluru", "Mexican, Cafe", 3.5, 12.9716, 77.5946)
        ));
    }

    @Nested
    class CreateRestaurant {

        @Test
        void createsAndPersistsRestaurantInMongo() throws Exception {
            String payload = """
                    {
                      "restaurantId": 101,
                      "restaurantName": "The Golden Fork",
                      "cuisines": "Italian, Pizza",
                      "location": {
                        "countryCode": 1,
                        "country": "India",
                        "city": "Delhi",
                        "address": "42 Connaught Place",
                        "latitude": 28.6315,
                        "longitude": 77.2167
                      },
                      "rating": {
                        "aggregateRating": 4.6,
                        "ratingColor": "Dark Green",
                        "ratingText": "Excellent",
                        "votes": 250
                      }
                    }
                    """;

            mockMvc.perform(post("/v1/restaurants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.restaurantId").value(101))
                    .andExpect(jsonPath("$.restaurantName").value("The Golden Fork"))
                    .andExpect(jsonPath("$.location.city").value("Delhi"))
                    .andExpect(jsonPath("$.rating.aggregateRating").value(4.6));

            Restaurant persisted = restaurantRepository.findById(101L).orElse(null);
            assertThat(persisted).isNotNull();
            assertThat(persisted.getRestaurantName()).isEqualTo("The Golden Fork");
            assertThat(persisted.getLocation().getCity()).isEqualTo("Delhi");
        }

        @Test
        void returns409WhenDuplicateRestaurantId() throws Exception {
            restaurantRepository.save(
                    buildRestaurant(101L, "Existing Place", "Delhi", "Cafe", 4.0, 28.6, 77.2));

            String duplicatePayload = """
                    {
                      "restaurantId": 101,
                      "restaurantName": "Duplicate Place",
                      "cuisines": "Italian"
                    }
                    """;

            mockMvc.perform(post("/v1/restaurants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(duplicatePayload))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(containsString("101")))
                    .andExpect(jsonPath("$.path").value("/v1/restaurants"));
        }

        @Test
        void returns400WhenValidationFails() throws Exception {
            String invalidPayload = """
                    {
                      "restaurantId": -5,
                      "restaurantName": "  ",
                      "location": {
                        "countryCode": 1,
                        "country": "India",
                        "city": "Delhi",
                        "address": "123 St",
                        "latitude": 95.0,
                        "longitude": 77.2
                      }
                    }
                    """;

            mockMvc.perform(post("/v1/restaurants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidPayload))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").isNotEmpty())
                    .andExpect(jsonPath("$.path").value("/v1/restaurants"));

            assertThat(restaurantRepository.count()).isZero();
        }

        @Test
        void returns400WhenJsonMalformed() throws Exception {
            mockMvc.perform(post("/v1/restaurants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{not-valid-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
        }
    }

    @Nested
    class GetRestaurantById {

        @Test
        void returns200WithFullDocumentWhenFound() throws Exception {
            restaurantRepository.save(
                    buildRestaurant(10L, "Bistro Central", "Mumbai", "French", 4.7, 19.0760, 72.8777));

            mockMvc.perform(get("/v1/restaurants/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurantId").value(10))
                    .andExpect(jsonPath("$.restaurantName").value("Bistro Central"))
                    .andExpect(jsonPath("$.location.city").value("Mumbai"))
                    .andExpect(jsonPath("$.rating.aggregateRating").value(4.7));
        }

        @Test
        void returns404WhenNotFound() throws Exception {
            mockMvc.perform(get("/v1/restaurants/9999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.path").value("/v1/restaurants/9999"));
        }

        @Test
        void returns400WhenIdNotPositive() throws Exception {
            mockMvc.perform(get("/v1/restaurants/0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Restaurant ID must be positive"));
        }

        @Test
        void returns400WhenIdNotNumeric() throws Exception {
            mockMvc.perform(get("/v1/restaurants/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("abc")));
        }
    }

    @Nested
    class SearchRestaurants {

        @Test
        void returnsAllSortedByNameAscByDefault() throws Exception {
            seedSearchDataset();

            mockMvc.perform(get("/v1/restaurants"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)))
                    .andExpect(jsonPath("$[*].name", containsInRelativeOrder(
                            "Alpha Bistro", "Beta Spice", "Delta Tacos", "Gamma Sushi")));
        }

        @Test
        void filtersByNameCityCuisinesAndRatingRange() throws Exception {
            seedSearchDataset();

            mockMvc.perform(get("/v1/restaurants").param("name", "spice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Beta Spice"));

            mockMvc.perform(get("/v1/restaurants").param("city", "delhi"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInRelativeOrder("Alpha Bistro", "Beta Spice")));

            mockMvc.perform(get("/v1/restaurants").param("cuisines", "cafe, japanese"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[*].name", containsInRelativeOrder(
                            "Alpha Bistro", "Delta Tacos", "Gamma Sushi")));

            mockMvc.perform(get("/v1/restaurants")
                            .param("minRating", "4.2")
                            .param("maxRating", "4.6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].name", containsInRelativeOrder("Alpha Bistro", "Beta Spice")));
        }

        @Test
        void filtersAndSortsByGeoDistance() throws Exception {
            seedSearchDataset();

            mockMvc.perform(get("/v1/restaurants")
                            .param("latitude", "28.6139")
                            .param("longitude", "77.2090")
                            .param("maxDistance", "15")
                            .param("sortBy", "distance")
                            .param("sortOrder", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("Alpha Bistro"))
                    .andExpect(jsonPath("$[0].distance", closeTo(0.0, 0.01)))
                    .andExpect(jsonPath("$[1].name").value("Beta Spice"))
                    .andExpect(jsonPath("$[1].distance", greaterThan(0.0)))
                    .andExpect(jsonPath("$[1].distance", lessThanOrEqualTo(15.0)));
        }

        @Test
        void sortsByRatingDescAndCityAscAndFallsBackWhenDistanceWithoutCoords() throws Exception {
            seedSearchDataset();

            mockMvc.perform(get("/v1/restaurants")
                            .param("sortBy", "rating")
                            .param("sortOrder", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", containsInRelativeOrder(
                            "Gamma Sushi", "Alpha Bistro", "Beta Spice", "Delta Tacos")));

            mockMvc.perform(get("/v1/restaurants")
                            .param("sortBy", "city")
                            .param("sortOrder", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].city").value("Bengaluru"))
                    .andExpect(jsonPath("$[3].city").value("Mumbai"));

            mockMvc.perform(get("/v1/restaurants")
                            .param("sortBy", "distance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", containsInRelativeOrder(
                            "Alpha Bistro", "Beta Spice", "Delta Tacos", "Gamma Sushi")));
        }

        @Test
        void returns400ForInvalidSearchParameters() throws Exception {
            mockMvc.perform(get("/v1/restaurants")
                            .param("minRating", "4.5")
                            .param("maxRating", "3.0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            mockMvc.perform(get("/v1/restaurants")
                            .param("latitude", "28.6"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            mockMvc.perform(get("/v1/restaurants")
                            .param("latitude", "91.0")
                            .param("longitude", "77.0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            mockMvc.perform(get("/v1/restaurants")
                            .param("maxDistance", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            mockMvc.perform(get("/v1/restaurants")
                            .param("sortBy", "invalidField"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));

            mockMvc.perform(get("/v1/restaurants")
                            .param("sortOrder", "sideways"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
