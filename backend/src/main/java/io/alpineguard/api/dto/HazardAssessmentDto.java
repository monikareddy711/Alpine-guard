package io.alpineguard.api.dto;

import io.alpineguard.domain.RouteHazardAssessment;
import io.alpineguard.domain.SegmentHazard;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record HazardAssessmentDto(
        String routeSlug,
        String routeName,
        String region,
        Instant generatedAt,
        int windowHours,
        double maxScore,
        double meanScore,
        String severity,
        double maxSlopeDeg,
        int steepSegmentCount,
        int totalSegments,
        List<SegmentHazard> segments,
        Map<String, Object> geoJson) {

    public static HazardAssessmentDto of(RouteHazardAssessment assessment, Map<String, Object> geoJson) {
        return new HazardAssessmentDto(assessment.routeSlug(), assessment.routeName(), assessment.region(),
                assessment.generatedAt(), assessment.windowHours(), assessment.maxScore(), assessment.meanScore(),
                assessment.severity().name(), assessment.maxSlopeDeg(), assessment.steepSegmentCount(),
                assessment.totalSegments(), assessment.segments(), geoJson);
    }
}
