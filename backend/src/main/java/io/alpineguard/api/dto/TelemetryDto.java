package io.alpineguard.api.dto;

import java.time.Instant;
import java.util.List;

public record TelemetryDto(
        String stationId,
        String name,
        int hours,
        Instant from,
        Instant to,
        List<Instant> timestamps,
        List<Double> airTempC,
        List<Double> windSpeedMs,
        List<Double> windGustMs,
        List<Double> windDirDeg,
        List<Double> snowDepthCm,
        List<Double> newSnowCm) {
}
