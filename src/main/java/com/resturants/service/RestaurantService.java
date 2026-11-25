package com.resturants.service;

import com.resturants.dto.RestaurantListResponse;
import com.resturants.exception.DuplicateRestaurantException;
import com.resturants.exception.RestaurantNotFoundException;
import com.resturants.model.Restaurant;
import com.resturants.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

@Service
public class RestaurantService {

    private static final Set<String> VALID_SORT_FIELDS = Set.of("name", "rating", "distance", "city");
    private static final Set<String> VALID_SORT_ORDERS = Set.of("asc", "desc");

    private final RestaurantRepository repository;

    public RestaurantService(RestaurantRepository repository) {
        this.repository = repository;
    }

    public Restaurant add(Restaurant restaurant) {
        if (restaurant.getRestaurantId() != null && repository.existsById(restaurant.getRestaurantId())) {
            throw new DuplicateRestaurantException(restaurant.getRestaurantId());
        }
        return repository.save(restaurant);
    }

    public Restaurant findById(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Restaurant ID must be positive");
        }
        return repository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));
    }

    public List<RestaurantListResponse> searchRestaurants(String name,
            String city,
            String cuisines,
            Double minRating,
            Double maxRating,
            Double latitude,
            Double longitude,
            Double maxDistance,
            String sortBy,
            String sortOrder) {

        validateListParams(minRating, maxRating, latitude, longitude, maxDistance, sortBy, sortOrder);

        boolean hasCoords = latitude != null && longitude != null;

        Stream<RestaurantListResponse> stream = repository.findAll().stream()
                .map(r -> toDto(r, hasCoords ? latitude : null, hasCoords ? longitude : null));

        if (name != null && !name.isBlank()) {
            String lowerName = name.toLowerCase();
            stream = stream.filter(dto -> dto.name() != null
                    && dto.name().toLowerCase().contains(lowerName));
        }

        if (city != null && !city.isBlank()) {
            String lowerCity = city.toLowerCase();
            stream = stream.filter(dto -> dto.city() != null
                    && dto.city().equalsIgnoreCase(lowerCity));
        }

        if (cuisines != null && !cuisines.isBlank()) {
            Set<String> wanted = new HashSet<>();
            for (String c : cuisines.split(",")) {
                wanted.add(c.trim().toLowerCase());
            }
            stream = stream.filter(dto -> {
                if (dto.cuisines() == null)
                    return false;
                for (String c : dto.cuisines().split(",")) {
                    if (wanted.contains(c.trim().toLowerCase()))
                        return true;
                }
                return false;
            });
        }

        if (minRating != null) {
            stream = stream.filter(dto -> dto.averageRating() >= minRating);
        }
        if (maxRating != null) {
            stream = stream.filter(dto -> dto.averageRating() <= maxRating);
        }

        if (hasCoords && maxDistance != null) {
            stream = stream.filter(dto -> dto.distance() != null
                    && dto.distance() <= maxDistance);
        }

        Comparator<RestaurantListResponse> comparator = buildComparator(sortBy, hasCoords);
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        return stream.sorted(comparator).toList();
    }

    private RestaurantListResponse toDto(Restaurant r, Double userLat, Double userLon) {
        Double distance = null;
        if (userLat != null && userLon != null && r.getLocation() != null) {
            distance = haversineKm(userLat, userLon,
                    r.getLocation().getLatitude(), r.getLocation().getLongitude());
        }

        return new RestaurantListResponse(
                r.getRestaurantId() != null ? r.getRestaurantId() : 0L,
                r.getRestaurantName(),
                r.getLocation() != null ? r.getLocation().getAddress() : null,
                r.getLocation() != null ? r.getLocation().getCity() : null,
                r.getLocation() != null ? r.getLocation().getCountry() : null,
                r.getCuisines(),
                r.getRating() != null ? r.getRating().getAggregateRating() : 0.0,
                distance);
    }

    /**
     * Haversine formula – returns the great-circle distance in <b>kilometres</b>.
     */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6_371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private void validateListParams(Double minRating,
            Double maxRating,
            Double latitude,
            Double longitude,
            Double maxDistance,
            String sortBy,
            String sortOrder) {
        if (minRating != null && maxRating != null && minRating > maxRating) {
            throw new IllegalArgumentException("minRating cannot be greater than maxRating");
        }
        if ((latitude != null) != (longitude != null)) {
            throw new IllegalArgumentException("Both latitude and longitude must be provided together");
        }
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
        if (maxDistance != null && maxDistance < 0) {
            throw new IllegalArgumentException("maxDistance cannot be negative");
        }
        if (sortBy != null && !sortBy.isBlank() && !VALID_SORT_FIELDS.contains(sortBy.toLowerCase())) {
            throw new IllegalArgumentException("Invalid sortBy value: " + sortBy);
        }
        if (sortOrder != null && !sortOrder.isBlank() && !VALID_SORT_ORDERS.contains(sortOrder.toLowerCase())) {
            throw new IllegalArgumentException("Invalid sortOrder value: " + sortOrder);
        }
    }

    private Comparator<RestaurantListResponse> buildComparator(String sortBy, boolean hasCoords) {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "name"; // default
        }

        return switch (sortBy.toLowerCase()) {
            case "rating" -> Comparator.comparingDouble(RestaurantListResponse::averageRating);
            case "distance" -> {
                if (!hasCoords)
                    yield Comparator.comparing(RestaurantListResponse::name,
                            Comparator.nullsLast(String::compareToIgnoreCase));
                yield Comparator.comparingDouble(dto -> dto.distance() != null ? dto.distance() : Double.MAX_VALUE);
            }
            case "city" -> Comparator.comparing(RestaurantListResponse::city,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            default -> Comparator.comparing(RestaurantListResponse::name,
                    Comparator.nullsLast(String::compareToIgnoreCase));
        };
    }
}
