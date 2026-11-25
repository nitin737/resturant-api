package com.resturants.Resturants_api.controller;

import com.resturants.Resturants_api.exception.DuplicateRestaurantException;
import com.resturants.Resturants_api.exception.GlobalExceptionHandler;
import com.resturants.Resturants_api.exception.RestaurantNotFoundException;
import com.resturants.Resturants_api.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RestaurantControllerExceptionTest {

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
    class ClientErrors4xx {

        @Test
        void returns404WhenRestaurantNotFound() throws Exception {
            when(service.findById(99L)).thenThrow(new RestaurantNotFoundException(99L));

            mockMvc.perform(get("/v1/restaurants/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Restaurant not found with id: 99"))
                    .andExpect(jsonPath("$.path").value("/v1/restaurants/99"));
        }

        @Test
        void returns409WhenDuplicateRestaurant() throws Exception {
            when(service.add(any())).thenThrow(new DuplicateRestaurantException(1L));

            mockMvc.perform(post("/v1/restaurants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"restaurantId\":1,\"restaurantName\":\"Cafe\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("Restaurant already exists with id: 1"));
        }

        @Test
        void returns400WhenInvalidQueryParam() throws Exception {
            when(service.searchRestaurants(null, null, null, 5.0, 2.0, null, null, null, "name", "asc"))
                    .thenThrow(new IllegalArgumentException("minRating cannot be greater than maxRating"));

            mockMvc.perform(get("/v1/restaurants")
                            .param("minRating", "5.0")
                            .param("maxRating", "2.0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("minRating cannot be greater than maxRating"));
        }

        @Test
        void returns400WhenPathVariableTypeMismatch() throws Exception {
            mockMvc.perform(get("/v1/restaurants/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));
        }

        @Test
        void returns400WhenQueryParamTypeMismatch() throws Exception {
            mockMvc.perform(get("/v1/restaurants").param("minRating", "not-a-number"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Invalid value 'not-a-number' for parameter 'minRating'"));
        }

        @Test
        void returns400WhenRequestBodyFailsModelValidation() throws Exception {
            mockMvc.perform(post("/v1/restaurants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"restaurantId\":-1,\"restaurantName\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("restaurantName")))
                    .andExpect(jsonPath("$.message").value(containsString("restaurantId")));
        }

        @Test
        void returns400WhenRequestBodyIsMalformedJson() throws Exception {
            mockMvc.perform(post("/v1/restaurants")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
        }
    }

    @Nested
    class ServerErrors5xx {

        @Test
        void returns503WhenDatabaseFails() throws Exception {
            when(service.findById(1L)).thenThrow(new DataAccessResourceFailureException("Mongo down"));

            mockMvc.perform(get("/v1/restaurants/1"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503))
                    .andExpect(jsonPath("$.message").value("Database error occurred"));
        }

        @Test
        void returns500WhenUnexpectedExceptionOccurs() throws Exception {
            when(service.findById(1L)).thenThrow(new RuntimeException("boom"));

            mockMvc.perform(get("/v1/restaurants/1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        }
    }
}
