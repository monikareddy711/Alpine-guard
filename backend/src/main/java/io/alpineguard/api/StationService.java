package io.alpineguard.api;

import io.alpineguard.api.dto.GeoJsonFeatureCollectionBuilder;
import io.alpineguard.api.dto.ReadingDto;
import io.alpineguard.api.dto.StationDto;
import io.alpineguard.api.dto.TelemetryDto;
import io.alpineguard.domain.ElevationBand;
import io.alpineguard.domain.Station;
import io.alpineguard.domain.StationReading;
import io.alpineguard.repository.BulletinRepository;
import io.alpineguard.repository.ReadingAggregate;
import io.alpineguard.repository.ReadingRepository;
import io.alpineguard.repository.StationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StationService {

    private final StationRepository stationRepository;
    private final ReadingRepository readingRepository;
    private final BulletinRepository bulletinRepository;

    public StationService(StationRepository stationRepository, ReadingRepository readingRepository,
                          BulletinRepository bulletinRepository) {
        this.stationRepository = stationRepository;
        this.readingRepository = readingRepository;
        this.bulletinRepository = bulletinRepository;
    }

    public List<StationDto> list(String region, Double minElevation, Double maxElevation,
                                 ElevationBand band, Integer minDanger) {
        Instant reference = readingRepository.latestTimestamp().orElse(Instant.now());
        Map<String, ReadingAggregate> aggregates = readingRepository.aggregate(Instant.EPOCH,
                reference.minus(Duration.ofHours(24)));
        Map<String, Integer> ratings = bulletinRepository.latestRatings();

        List<StationDto> result = new ArrayList<>();
        for (Station station : stationRepository.findAllActive()) {
            if (region != null && !region.isBlank() && !station.region().equalsIgnoreCase(region)) {
                continue;
            }
            if (minElevation != null && station.elevationM() < minElevation) {
                continue;
            }
            if (maxElevation != null && station.elevationM() > maxElevation) {
                continue;
            }
            if (band != null && station.band() != band) {
                continue;
            }
            Integer rating = ratings.get(station.region() + "|" + station.band().name());
            if (minDanger != null && (rating == null || rating < minDanger)) {
                continue;
            }
            ReadingAggregate aggregate = aggregates.get(station.stationId());
            result.add(StationDto.of(station, rating,
                    aggregate == null ? null : ReadingDto.from(aggregate.latest())));
        }
        return result;
    }

    public Optional<StationDto> byId(String stationId) {
        return stationRepository.findByStationId(stationId)
                .map(station -> StationDto.of(station, null, null));
    }

    @Cacheable(cacheNames = "telemetry", key = "#stationId + ':' + #hours")
    public TelemetryDto telemetry(String stationId, int hours) {
        Station station = stationRepository.findByStationId(stationId)
                .orElseThrow(() -> new IllegalArgumentException("unknown station: " + stationId));
        Instant to = readingRepository.latestTimestamp().orElse(Instant.now());
        Instant from = to.minus(Duration.ofHours(hours));
        List<StationReading> series = readingRepository.series(stationId, from, to);
        List<Instant> timestamps = new ArrayList<>(series.size());
        List<Double> airTemp = new ArrayList<>(series.size());
        List<Double> windSpeed = new ArrayList<>(series.size());
        List<Double> windGust = new ArrayList<>(series.size());
        List<Double> windDir = new ArrayList<>(series.size());
        List<Double> snowDepth = new ArrayList<>(series.size());
        List<Double> newSnow = new ArrayList<>(series.size());
        for (StationReading reading : series) {
            timestamps.add(reading.measuredAt());
            airTemp.add(reading.airTempC());
            windSpeed.add(reading.windSpeedMs());
            windGust.add(reading.windGustMs());
            windDir.add(reading.windDirDeg());
            snowDepth.add(reading.snowDepthCm());
            newSnow.add(reading.newSnowCm());
        }
        return new TelemetryDto(stationId, station.name(), hours, from, to, timestamps, airTemp, windSpeed,
                windGust, windDir, snowDepth, newSnow);
    }

    public Map<String, Object> featureCollection() {
        Instant reference = readingRepository.latestTimestamp().orElse(Instant.now());
        Map<String, ReadingAggregate> aggregates = readingRepository.aggregate(Instant.EPOCH,
                reference.minus(Duration.ofHours(24)));
        Map<String, Integer> ratings = bulletinRepository.latestRatings();
        GeoJsonFeatureCollectionBuilder builder = GeoJsonFeatureCollectionBuilder.create();
        for (Station station : stationRepository.findAllActive()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("stationId", station.stationId());
            properties.put("name", station.name());
            properties.put("region", station.region());
            properties.put("elevationM", station.elevationM());
            properties.put("elevationBand", station.band().name());
            Integer rating = ratings.get(station.region() + "|" + station.band().name());
            properties.put("dangerRating", rating);
            properties.put("severity", StationDto.severityOf(rating).name());
            ReadingAggregate aggregate = aggregates.get(station.stationId());
            if (aggregate != null) {
                properties.put("snowDepthCm", aggregate.latest().snowDepthCm());
                properties.put("windGustMs", aggregate.latest().windGustMs());
                properties.put("newSnow24hCm", aggregate.newSnow24hCm());
            }
            builder.point(station.lon(), station.lat(), properties);
        }
        return builder.build();
    }
}
