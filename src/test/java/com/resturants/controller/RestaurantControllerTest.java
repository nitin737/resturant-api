package com.resturants.controller;

import com.resturants.dto.RestaurantListResponse;
import com.resturants.exception.GlobalExceptionHandler;
import com.resturants.model.Restaurant;
import com.resturants.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RestaurantControllerTest {

    @Mock
    private RestaurantService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RestaurantController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    class CreateRestaurant {

        @Test
        void returns201AndSavedRestaurant() throws Exception {
            Restaurant saved = Restaurant.builder()
                    .restaurantId(1L)
                    .restaurantName("The Golden Fork")
                    .cuisines("Italian")
                    .build();
            when(service.add(any(Restaurant.class))).thenReturn(saved);

            mockMvc.perform(post("/v1/restaurants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"restaurantId\":1,\"restaurantName\":\"The Golden Fork\",\"cuisines\":\"Italian\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.restaurantId").value(1))
                    .andExpect(jsonPath("$.restaurantName").value("The Golden Fork"))
                    .andExpect(jsonPath("$.cuisines").value("Italian"));
        }
    }

    @Nested
    class GetRestaurantById {

        @Test
        void returns200AndRestaurant() throws Exception {
            Restaurant restaurant = Restaurant.builder()
                    .restaurantId(10L)
                    .restaurantName("Bistro")
                    .build();
            when(service.findById(10L)).thenReturn(restaurant);

            mockMvc.perform(get("/v1/restaurants/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restaurantId").value(10))
                    .andExpect(jsonPath("$.restaurantName").value("Bistro"));
        }
    }

    @Nested
    class ListRestaurants {

        @Test
        void returns200WithDefaultSortParameters() throws Exception {
            RestaurantListResponse item = new RestaurantListResponse(
                    1L, "Bistro", "123 Main St", "Bengaluru", "India", "Italian", 4.5, null);
            when(service.searchRestaurants(null, null, null, null, null, null, null, null, "name", "asc"))
                    .thenReturn(List.of(item));

            mockMvc.perform(get("/v1/restaurants"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Bistro"))
                    .andExpect(jsonPath("$[0].averageRating").value(4.5));

            verify(service).searchRestaurants(null, null, null, null, null, null, null, null, "name", "asc");
        }

        @Test
        void forwardsQueryParametersToService() throws Exception {
            when(service.searchRestaurants("fork", "Bengaluru", "Italian", 3.5, 5.0,
                    12.97, 77.59, 10.0, "distance", "desc"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/v1/restaurants")
                            .param("name", "fork")
                            .param("city", "Bengaluru")
                            .param("cuisines", "Italian")
                            .param("minRating", "3.5")
                            .param("maxRating", "5.0")
                            .param("latitude", "12.97")
                            .param("longitude", "77.59")
                            .param("maxDistance", "10.0")
                            .param("sortBy", "distance")
                            .param("sortOrder", "desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(service).searchRestaurants("fork", "Bengaluru", "Italian", 3.5, 5.0,
                    12.97, 77.59, 10.0, "distance", "desc");
        }
    }
}
