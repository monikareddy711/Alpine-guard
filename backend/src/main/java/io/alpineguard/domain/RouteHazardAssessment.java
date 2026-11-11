package io.alpineguard.domain;

import java.time.Instant;
import java.util.List;

public record RouteHazardAssessment(
        String routeSlug,
        String routeName,
        String region,
        Instant generatedAt,
        int windowHours,
        double maxScore,
        double meanScore,
        HazardSeverity severity,
        double maxSlopeDeg,
        int steepSegmentCount,
        int totalSegments,
        List<SegmentHazard> segments) {
}
