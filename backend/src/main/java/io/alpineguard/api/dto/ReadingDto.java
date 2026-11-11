package io.alpineguard.api.dto;

import io.alpineguard.domain.StationReading;

import java.time.Instant;

public record ReadingDto(
        Instant measuredAt,
        Double airTempC,
        Double windSpeedMs,
        Double windGustMs,
        Double windDirDeg,
        Double snowDepthCm,
        Double newSnowCm,
        Double humidityPct,
        Double pressureHpa) {

    public static ReadingDto from(StationReading reading) {
        if (reading == null) {
            return null;
        }
        return new ReadingDto(reading.measuredAt(), reading.airTempC(), reading.windSpeedMs(), reading.windGustMs(),
                reading.windDirDeg(), reading.snowDepthCm(), reading.newSnowCm(), reading.humidityPct(),
                reading.pressureHpa());
    }
}
