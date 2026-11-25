# Restaurants API (`Resturants-api`)

A Spring Boot REST API for managing and searching restaurants, backed by MongoDB.

## Tech Stack

- **Java 21**
- **Spring Boot 4.2.0** (Web MVC, Data MongoDB, Bean Validation)
- **MongoDB 8**
- **Gradle** (via included Gradle Wrapper)
- **Lombok**
- **Docker & Docker Compose** (with Spring Boot Docker Compose support)
- **JUnit 5, Mockito & Testcontainers**

## Prerequisites

- **Java 21+** (if running or building outside Docker)
- **Docker & Docker Compose** (for running MongoDB / full stack containers)

## Getting Started

### Option 1: Run the Full Stack with Docker Compose

Build and start both the API (`restaurants-api`) and MongoDB (`restaurants-mongo`):

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080`.

To stop the services:

```bash
docker compose down
```

### Option 2: Run Locally with Gradle

Because `spring-boot-docker-compose` is included in `developmentOnly`, running the application with Gradle automatically manages the `mongodb` container defined in `compose.yaml` (Docker must be running):

**macOS / Linux:**
```bash
./gradlew bootRun
```

**Windows (PowerShell / CMD):**
```powershell
.\gradlew.bat bootRun
```

#### Using an External MongoDB Instance

If you prefer to connect to your own MongoDB instance instead of the Docker Compose container, set `SPRING_MONGODB_URI`:

```bash
SPRING_MONGODB_URI="mongodb://localhost:27017/restaurants" ./gradlew bootRun
```

In `compose.yaml`, the containerized app connects using:
```text
mongodb://mongo:secret@mongodb:27017/restaurants?authSource=admin
```

## Building and Testing

### Run Tests

```bash
./gradlew test
```

On Windows:
```powershell
.\gradlew.bat test
```

> **Note:** `ResturantsApiApplicationTests` loads the full `@SpringBootTest` context, so ensure Docker or a local MongoDB instance is available when running the full test suite.

### Build Executable JAR

```bash
./gradlew bootJar
```

The JAR is output to `build/libs/`.

## API Reference

Base path: `/v1/restaurants`

### 1. Create a Restaurant

- **POST** `/v1/restaurants`
- **Status Codes:** `201 Created`, `400 Bad Request` (validation error), `409 Conflict` (duplicate `restaurantId`)

Example request body:

```json
{
  "restaurantId": 1001,
  "restaurantName": "The Spice Garden",
  "cuisines": "Indian, North Indian",
  "location": {
    "countryCode": 1,
    "country": "India",
    "city": "Bengaluru",
    "address": "123 MG Road",
    "locality": "MG Road",
    "localityVerbose": "MG Road, Bengaluru",
    "longitude": 77.6092,
    "latitude": 12.9756
  },
  "pricing": {
    "averageCostForTwo": 1200,
    "currency": "INR",
    "priceRange": 3
  },
  "features": {
    "hasTableBooking": true,
    "hasOnlineDelivery": true,
    "isDeliveringNow": false,
    "switchToOrderMenu": false
  },
  "rating": {
    "aggregateRating": 4.5,
    "ratingColor": "Dark Green",
    "ratingText": "Excellent",
    "votes": 340
  }
}
```

### 2. Get Restaurant by ID

- **GET** `/v1/restaurants/{id}`
- **Status Codes:** `200 OK`, `400 Bad Request` (non-positive ID), `404 Not Found`

### 3. Search & Filter Restaurants

- **GET** `/v1/restaurants`
- **Query Parameters** (all optional):
  - `name` — Case-insensitive substring match on restaurant name
  - `city` — Case-insensitive exact match on city
  - `cuisines` — Comma-separated list of cuisines (matches if any cuisine matches)
  - `minRating` / `maxRating` — Filter by `aggregateRating` range
  - `latitude` & `longitude` — User coordinates (must be provided together; calculates Haversine distance in km)
  - `maxDistance` — Maximum distance in km (requires `latitude` and `longitude`)
  - `sortBy` — `name` (default), `rating`, `distance`, or `city`
  - `sortOrder` — `asc` (default) or `desc`

Example:

```bash
curl "http://localhost:8080/v1/restaurants?city=Bengaluru&minRating=4.0&sortBy=rating&sortOrder=desc"
```

### Error Responses

All errors return a consistent JSON structure (`ApiError`):

```json
{
  "timestamp": "2026-01-01T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Restaurant not found with id: 999",
  "path": "/v1/restaurants/999"
}
```

## Project Structure

```text
src/main/java/com/resturants/
├── ResturantsApiApplication.java          # Spring Boot entry point
├── controller/RestaurantController.java
├── dto/RestaurantListResponse.java
├── exception/                         # GlobalExceptionHandler & custom exceptions
├── model/                             # Restaurant, Location, Pricing, Features, Rating
├── repository/RestaurantRepository.java
└── service/RestaurantService.java
```
