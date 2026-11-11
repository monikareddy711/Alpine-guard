package io.alpineguard.api.dto;

import java.util.List;
import java.util.Map;

public record RouteDetailDto(
        String slug,
        String name,
        String region,
        String difficulty,
        double distanceM,
        double ascentM,
        double descentM,
        double maxElevationM,
        double minElevationM,
        List<RoutePointDto> points,
        Map<String, Object> geoJson) {
}
