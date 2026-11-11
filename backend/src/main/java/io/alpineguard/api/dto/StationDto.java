package io.alpineguard.api.dto;

import io.alpineguard.domain.ElevationBand;
import io.alpineguard.domain.HazardSeverity;
import io.alpineguard.domain.Station;

public record StationDto(
        String stationId,
        String name,
        String region,
        String operator,
        String network,
        double elevationM,
        ElevationBand elevationBand,
        double lat,
        double lon,
        boolean active,
        Integer dangerRating,
        HazardSeverity severity,
        ReadingDto latestReading) {

    public static StationDto of(Station station, Integer dangerRating, ReadingDto reading) {
        return new StationDto(station.stationId(), station.name(), station.region(), station.operator(),
                station.network(), station.elevationM(), station.band(), station.lat(), station.lon(),
                station.active(), dangerRating, severityOf(dangerRating), reading);
    }

    public static HazardSeverity severityOf(Integer dangerRating) {
        if (dangerRating == null) {
            return HazardSeverity.LOW;
        }
        return switch (dangerRating) {
            case 1 -> HazardSeverity.LOW;
            case 2 -> HazardSeverity.MODERATE;
            case 3 -> HazardSeverity.CONSIDERABLE;
            case 4 -> HazardSeverity.HIGH;
            default -> HazardSeverity.EXTREME;
        };
    }
}
