package com.resturants.Resturants_api;

import com.resturants.Resturants_api.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mongodb.MongoDBContainer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.ingestion.enabled=false"
)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MongoDBContainer MONGO_CONTAINER = new MongoDBContainer("mongo:8");

    static {
        MONGO_CONTAINER.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected RestaurantRepository restaurantRepository;

    @BeforeEach
    void cleanDatabase() {
        restaurantRepository.deleteAll();
    }
}
