package io.alpineguard.spatial;

import java.util.List;
import java.util.Optional;

/**
 * Proximity lookup for weather stations. Two implementations exist: one pushes the
 * radius filter into the spatial index of the database, the other keeps the station
 * coordinates in a JVM k-d tree. Results are ordered by distance and are identical
 * between both implementations.
 */
public interface SpatialQueryStrategy {

    String name();

    List<StationProximity> within(double lat, double lon, double radiusM);

    Optional<StationProximity> nearest(double lat, double lon, double radiusM);
}
