# AI Usage

This document outlines how AI tools were used during the development of this repository.

1. **Controller Layer Boilerplate**
   - Used to generate boilerplate controller layer methods in `src/main/java/com/resturants/controller/RestaurantController.java`.

2. **Haversine Distance Logic**
   - Used to generate the logic for the `haversineKm` method in `src/main/java/com/resturants/service/RestaurantService.java` to calculate great-circle distance in kilometres.

3. **Model Validation Tests**
   - Used to generate tests for `src/test/java/com/resturants/model/RestaurantValidationTest.java`, as writing repetitive validation test cases requires more time and effort.

4. **Integration Test Setup & Test Data**
   - Used to set up the integration test infrastructure and test data in this repository (such as `src/test/java/com/resturants/AbstractIntegrationTest.java` and test data builders/seeding in `src/test/java/com/resturants/controller/RestaurantControllerIT.java`), excluding the integration test cases themselves.

5. **Docker & Docker Compose Setup**
   - Used to set up the `Dockerfile` and Docker Compose configuration in this project.
