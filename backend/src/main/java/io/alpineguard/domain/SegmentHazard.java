package io.alpineguard.domain;

import java.util.List;

public record SegmentHazard(
        int index,
        double startLat,
        double startLon,
        double endLat,
        double endLon,
        double elevationM,
        double slopeDeg,
        double aspectDeg,
        double score,
        HazardSeverity severity,
        ElevationBand band,
        String nearestStationId,
        String nearestStationName,
        double nearestStationDistanceM,
        double leeAlignment,
        List<String> flags) {
}
