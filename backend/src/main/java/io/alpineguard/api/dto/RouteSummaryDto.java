package io.alpineguard.api.dto;

import io.alpineguard.domain.HazardSeverity;

public record RouteSummaryDto(
        String slug,
        String name,
        String region,
        String difficulty,
        double distanceM,
        double ascentM,
        double descentM,
        double maxElevationM,
        double minElevationM,
        double maxScore,
        double meanScore,
        HazardSeverity severity,
        double maxSlopeDeg,
        int steepSegmentCount,
        int totalSegments) {
}
