package com.resturants.Resturants_api.service;

import com.resturants.Resturants_api.dto.RestaurantListResponse;
import com.resturants.Resturants_api.exception.DuplicateRestaurantException;
import com.resturants.Resturants_api.exception.RestaurantNotFoundException;
import com.resturants.Resturants_api.model.Location;
import com.resturants.Resturants_api.model.Rating;
import com.resturants.Resturants_api.model.Restaurant;
import com.resturants.Resturants_api.repository.RestaurantRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository repository;

    @InjectMocks
    private RestaurantService service;

    private Restaurant restaurant(Long id, String name, String city, String cuisines,
                                  double rating, Double lat, Double lon) {
        Location location = (lat != null && lon != null)
                ? Location.builder()
                        .countryCode(1)
                        .country("India")
                        .city(city)
                        .address("123 Main St")
                        .latitude(lat)
                        .longitude(lon)
                        .build()
                : null;

        return Restaurant.builder()
                .restaurantId(id)
                .restaurantName(name)
                .cuisines(cuisines)
                .location(location)
                .rating(Rating.builder().aggregateRating(rating).votes(10).build())
                .build();
    }

    @Nested
    class AddTests {

        @Test
        void savesNewRestaurantWhenIdDoesNotExist() {
            Restaurant input = restaurant(10L, "Bistro", "Bengaluru", "Italian", 4.2, 12.97, 77.59);
            when(repository.existsById(10L)).thenReturn(false);
            when(repository.save(input)).thenReturn(input);

            Restaurant saved = service.add(input);

            assertEquals(input, saved);
            verify(repository).existsById(10L);
            verify(repository).save(input);
        }

        @Test
        void savesRestaurantWithoutExistenceCheckWhenIdIsNull() {
            Restaurant input = restaurant(null, "Bistro", "Bengaluru", "Italian", 4.2, 12.97, 77.59);
            when(repository.save(input)).thenReturn(input);

            Restaurant saved = service.add(input);

            assertEquals(input, saved);
            verify(repository, never()).existsById(org.mockito.ArgumentMatchers.any());
            verify(repository).save(input);
        }

        @Test
        void throwsDuplicateWhenIdAlreadyExists() {
            Restaurant existing = Restaurant.builder()
                    .restaurantId(10L)
                    .restaurantName("Bistro")
                    .build();
            when(repository.existsById(10L)).thenReturn(true);

            assertThrows(DuplicateRestaurantException.class, () -> service.add(existing));
            verify(repository, never()).save(existing);
        }
    }

    @Nested
    class FindByIdTests {

        @Test
        void returnsRestaurantWhenFound() {
            Restaurant expected = restaurant(1L, "Bistro", "Bengaluru", "Italian", 4.5, 12.97, 77.59);
            when(repository.findById(1L)).thenReturn(Optional.of(expected));

            Restaurant actual = service.findById(1L);

            assertEquals(expected, actual);
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(repository.findById(42L)).thenReturn(Optional.empty());

            RestaurantNotFoundException ex = assertThrows(
                    RestaurantNotFoundException.class,
                    () -> service.findById(42L));

            assertEquals("Restaurant not found with id: 42", ex.getMessage());
        }

        @Test
        void throwsIllegalArgumentWhenNonPositive() {
            assertThrows(IllegalArgumentException.class, () -> service.findById(0L));
            assertThrows(IllegalArgumentException.class, () -> service.findById(-5L));
        }
    }

    @Nested
    class SearchValidationTests {

        @Test
        void throwsWhenMinRatingExceedsMaxRating() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, 4.5, 3.0, null, null, null, "name", "asc"));
        }

        @Test
        void throwsWhenOnlyOneCoordinateProvided() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, null, null, 12.9, null, null, "name", "asc"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, null, null, null, 77.5, null, "name", "asc"));
        }

        @Test
        void throwsWhenLatitudeOutOfRange() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, null, null, 90.1, 77.0, null, "name", "asc"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, null, null, -90.1, 77.0, null, "name", "asc"));
        }

        @Test
        void throwsWhenLongitudeOutOfRange() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, null, null, 12.9, 180.1, null, "name", "asc"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, null, null, 12.9, -180.1, null, "name", "asc"));
        }

        @Test
        void throwsWhenMaxDistanceNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, null, null, null, null, -1.0, "name", "asc"));
        }

        @Test
        void throwsWhenSortByInvalid() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, null, null, null, null, null, "unknown", "asc"));
        }

        @Test
        void throwsWhenSortOrderInvalid() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.searchRestaurants(null, null, null, null, null, null, null, null, "name", "sideways"));
        }
    }

    @Nested
    class SearchFilteringTests {

        @Test
        void returnsEmptyListWhenRepositoryIsEmpty() {
            when(repository.findAll()).thenReturn(List.of());

            List<RestaurantListResponse> results = service.searchRestaurants(
                    null, null, null, null, null, null, null, null, "name", "asc");

            assertTrue(results.isEmpty());
        }

        @Test
        void mapsNullNestedFieldsToSafeDefaults() {
            Restaurant minimal = Restaurant.builder()
                    .restaurantId(null)
                    .restaurantName("Minimal")
                    .cuisines(null)
                    .location(null)
                    .rating(null)
                    .build();
            when(repository.findAll()).thenReturn(List.of(minimal));

            List<RestaurantListResponse> results = service.searchRestaurants(
                    null, null, null, null, null, 12.97, 77.59, null, "name", "asc");

            assertEquals(1, results.size());
            RestaurantListResponse dto = results.get(0);
            assertEquals(0L, dto.id());
            assertEquals("Minimal", dto.name());
            assertNull(dto.address());
            assertNull(dto.city());
            assertNull(dto.country());
            assertEquals(0.0, dto.averageRating());
            assertNull(dto.distance());
        }

        @Test
        void filtersByNameCaseInsensitiveSubstringAndIgnoresNullNames() {
            Restaurant match = restaurant(1L, "The Golden Fork", "Bengaluru", "Italian", 4.5, 12.97, 77.59);
            Restaurant other = restaurant(2L, "Silver Spoon", "Bengaluru", "Indian", 4.0, 12.97, 77.59);
            Restaurant nullName = restaurant(3L, null, "Bengaluru", "Indian", 4.0, 12.97, 77.59);
            when(repository.findAll()).thenReturn(List.of(match, other, nullName));

            List<RestaurantListResponse> results = service.searchRestaurants(
                    "golden", null, null, null, null, null, null, null, "name", "asc");

            assertEquals(List.of(1L), results.stream().map(RestaurantListResponse::id).toList());
        }

        @Test
        void filtersByCityCaseInsensitiveExactMatch() {
            Restaurant exact = restaurant(1L, "Alpha", "Bengaluru", "Italian", 4.5, 12.97, 77.59);
            Restaurant partial = restaurant(2L, "Beta", "Bengaluru North", "Italian", 4.5, 12.97, 77.59);
            Restaurant noLoc = restaurant(3L, "Gamma", null, "Italian", 4.5, null, null);
            when(repository.findAll()).thenReturn(List.of(exact, partial, noLoc));

            List<RestaurantListResponse> results = service.searchRestaurants(
                    null, "bengaluru", null, null, null, null, null, null, "name", "asc");

            assertEquals(List.of(1L), results.stream().map(RestaurantListResponse::id).toList());
        }

        @Test
        void filtersByCuisinesCommaSeparatedTrimmedCaseInsensitive() {
            Restaurant italian = restaurant(1L, "Alpha", "Bengaluru", "Italian, Mediterranean", 4.5, 12.97, 77.59);
            Restaurant thai = restaurant(2L, "Beta", "Bengaluru", "Thai", 4.2, 12.97, 77.59);
            Restaurant indian = restaurant(3L, "Gamma", "Bengaluru", "Indian", 4.1, 12.97, 77.59);
            Restaurant noCuisine = restaurant(4L, "Delta", "Bengaluru", null, 4.0, 12.97, 77.59);
            when(repository.findAll()).thenReturn(List.of(italian, thai, indian, noCuisine));

            List<RestaurantListResponse> results = service.searchRestaurants(
                    null, null, " thai , ITALIAN ", null, null, null, null, null, "name", "asc");

            assertEquals(List.of(1L, 2L), results.stream().map(RestaurantListResponse::id).toList());
        }

        @Test
        void filtersByMinAndMaxRatingInclusive() {
            Restaurant low = restaurant(1L, "Low", "Bengaluru", "Indian", 2.9, 12.97, 77.59);
            Restaurant minBound = restaurant(2L, "Min", "Bengaluru", "Indian", 3.0, 12.97, 77.59);
            Restaurant maxBound = restaurant(3L, "Max", "Bengaluru", "Indian", 4.5, 12.97, 77.59);
            Restaurant high = restaurant(4L, "High", "Bengaluru", "Indian", 4.6, 12.97, 77.59);
            when(repository.findAll()).thenReturn(List.of(low, minBound, maxBound, high));

            List<RestaurantListResponse> results = service.searchRestaurants(
                    null, null, null, 3.0, 4.5, null, null, null, "name", "asc");

            assertEquals(List.of(3L, 2L), results.stream().map(RestaurantListResponse::id).toList());
        }

        @Test
        void filtersByMaxDistanceUsingHaversineAndExcludesNullLocations() {
            // Origin: (12.9716, 77.5946)
            Restaurant atOrigin = restaurant(1L, "Origin", "Bengaluru", "Indian", 4.0, 12.9716, 77.5946);
            // ~1.11 km north
            Restaurant nearby = restaurant(2L, "Nearby", "Bengaluru", "Indian", 4.0, 12.9816, 77.5946);
            // ~111 km north
            Restaurant far = restaurant(3L, "Far", "Bengaluru", "Indian", 4.0, 13.9716, 77.5946);
            Restaurant noLoc = restaurant(4L, "NoLoc", "Bengaluru", "Indian", 4.0, null, null);
            when(repository.findAll()).thenReturn(List.of(atOrigin, nearby, far, noLoc));

            List<RestaurantListResponse> results = service.searchRestaurants(
                    null, null, null, null, null, 12.9716, 77.5946, 5.0, "distance", "asc");

            assertEquals(List.of(1L, 2L), results.stream().map(RestaurantListResponse::id).toList());
            assertEquals(0.0, results.get(0).distance(), 0.001);
            assertNotNull(results.get(1).distance());
            assertTrue(results.get(1).distance() > 1.0 && results.get(1).distance() < 2.0);
        }

        @Test
        void ignoresMaxDistanceWhenCoordinatesAreOmitted() {
            Restaurant far = restaurant(1L, "Far", "Bengaluru", "Indian", 4.0, 28.6139, 77.2090);
            when(repository.findAll()).thenReturn(List.of(far));

            List<RestaurantListResponse> results = service.searchRestaurants(
                    null, null, null, null, null, null, null, 1.0, "name", "asc");

            assertEquals(1, results.size());
            assertNull(results.get(0).distance());
        }

        @Test
        void ignoresBlankStringFiltersAndCombinesActiveFilters() {
            Restaurant match = restaurant(1L, "Spice Garden", "Mumbai", "Indian, Chinese", 4.3, 19.0760, 72.8777);
            Restaurant wrongCity = restaurant(2L, "Spice Hub", "Delhi", "Indian", 4.5, 28.6139, 77.2090);
            Restaurant lowRating = restaurant(3L, "Spice Street", "Mumbai", "Indian", 3.5, 19.0760, 72.8777);
            when(repository.findAll()).thenReturn(List.of(match, wrongCity, lowRating));

            List<RestaurantListResponse> results = service.searchRestaurants(
                    "   ", "Mumbai", "Indian", 4.0, null, null, null, null, "name", "asc");

            assertEquals(List.of(1L), results.stream().map(RestaurantListResponse::id).toList());
        }
    }

    @Nested
    class SearchSortingTests {

        @Test
        void sortsByNameAscAndDescWithNullsLastInAsc() {
            Restaurant b = restaurant(1L, "beta", "City", "Indian", 4.0, 12.0, 77.0);
            Restaurant a = restaurant(2L, "Alpha", "City", "Indian", 4.0, 12.0, 77.0);
            Restaurant nullName = restaurant(3L, null, "City", "Indian", 4.0, 12.0, 77.0);
            when(repository.findAll()).thenReturn(List.of(b, nullName, a));

            List<RestaurantListResponse> asc = service.searchRestaurants(
                    null, null, null, null, null, null, null, null, null, "asc");
            assertEquals(List.of(2L, 1L, 3L), asc.stream().map(RestaurantListResponse::id).toList());

            List<RestaurantListResponse> desc = service.searchRestaurants(
                    null, null, null, null, null, null, null, null, "name", "DESC");
            assertEquals(List.of(3L, 1L, 2L), desc.stream().map(RestaurantListResponse::id).toList());
        }

        @Test
        void sortsByCityAscAndDesc() {
            Restaurant mumbai = restaurant(1L, "A", "Mumbai", "Indian", 4.0, 19.0, 72.8);
            Restaurant bengaluru = restaurant(2L, "B", "Bengaluru", "Indian", 4.0, 12.9, 77.5);
            when(repository.findAll()).thenReturn(List.of(mumbai, bengaluru));

            List<RestaurantListResponse> asc = service.searchRestaurants(
                    null, null, null, null, null, null, null, null, "city", "asc");
            assertEquals(List.of(2L, 1L), asc.stream().map(RestaurantListResponse::id).toList());

            List<RestaurantListResponse> desc = service.searchRestaurants(
                    null, null, null, null, null, null, null, null, "city", "desc");
            assertEquals(List.of(1L, 2L), desc.stream().map(RestaurantListResponse::id).toList());
        }

        @Test
        void sortsByRatingAscAndDesc() {
            Restaurant low = restaurant(1L, "A", "City", "Indian", 3.2, 12.0, 77.0);
            Restaurant high = restaurant(2L, "B", "City", "Indian", 4.8, 12.0, 77.0);
            when(repository.findAll()).thenReturn(List.of(high, low));

            List<RestaurantListResponse> asc = service.searchRestaurants(
                    null, null, null, null, null, null, null, null, "rating", "asc");
            assertEquals(List.of(1L, 2L), asc.stream().map(RestaurantListResponse::id).toList());

            List<RestaurantListResponse> desc = service.searchRestaurants(
                    null, null, null, null, null, null, null, null, "rating", "desc");
            assertEquals(List.of(2L, 1L), desc.stream().map(RestaurantListResponse::id).toList());
        }

        @Test
        void sortsByDistanceWithCoordsAndPlacesNullLocationLast() {
            Restaurant far = restaurant(1L, "Far", "City", "Indian", 4.0, 13.5, 77.5946);
            Restaurant near = restaurant(2L, "Near", "City", "Indian", 4.0, 12.98, 77.5946);
            Restaurant noLoc = restaurant(3L, "NoLoc", "City", "Indian", 4.0, null, null);
            when(repository.findAll()).thenReturn(List.of(far, noLoc, near));

            List<RestaurantListResponse> asc = service.searchRestaurants(
                    null, null, null, null, null, 12.9716, 77.5946, null, "distance", "asc");

            assertEquals(List.of(2L, 1L, 3L), asc.stream().map(RestaurantListResponse::id).toList());
        }

        @Test
        void fallsBackToNameSortWhenSortByDistanceWithoutCoords() {
            Restaurant b = restaurant(1L, "Beta", "City", "Indian", 4.0, 12.0, 77.0);
            Restaurant a = restaurant(2L, "Alpha", "City", "Indian", 4.0, 12.0, 77.0);
            when(repository.findAll()).thenReturn(List.of(b, a));

            List<RestaurantListResponse> results = service.searchRestaurants(
                    null, null, null, null, null, null, null, null, "distance", "asc");

            assertEquals(List.of(2L, 1L), results.stream().map(RestaurantListResponse::id).toList());
        }
    }
}
