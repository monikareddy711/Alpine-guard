package io.alpineguard.domain;

import java.time.Instant;

public record StationReading(
        String stationId,
        Instant measuredAt,
        Double airTempC,
        Double windSpeedMs,
        Double windGustMs,
        Double windDirDeg,
        Double snowDepthCm,
        Double newSnowCm,
        Double humidityPct,
        Double pressureHpa) {
}
