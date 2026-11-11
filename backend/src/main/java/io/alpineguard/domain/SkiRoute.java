package io.alpineguard.domain;

import java.util.List;

public record SkiRoute(
        Long id,
        String slug,
        String name,
        String region,
        String difficulty,
        double distanceM,
        double ascentM,
        double descentM,
        double maxElevationM,
        double minElevationM,
        String gpxResource,
        List<RoutePoint> points) {
}
