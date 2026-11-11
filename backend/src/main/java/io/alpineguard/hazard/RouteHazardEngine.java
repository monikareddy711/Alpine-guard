package io.alpineguard.hazard;

import io.alpineguard.config.SpatialProperties;
import io.alpineguard.domain.ElevationBand;
import io.alpineguard.domain.GeoMath;
import io.alpineguard.domain.HazardSeverity;
import io.alpineguard.domain.RouteHazardAssessment;
import io.alpineguard.domain.RoutePoint;
import io.alpineguard.domain.SegmentHazard;
import io.alpineguard.domain.SkiRoute;
import io.alpineguard.repository.BulletinRepository;
import io.alpineguard.repository.ReadingAggregate;
import io.alpineguard.repository.ReadingRepository;
import io.alpineguard.repository.RouteRepository;
import io.alpineguard.spatial.SpatialQueryService;
import io.alpineguard.spatial.StationProximity;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RouteHazardEngine {

    public static final int DEFAULT_WINDOW_HOURS = 72;

    private final RouteRepository routeRepository;
    private final ReadingRepository readingRepository;
    private final BulletinRepository bulletinRepository;
    private final HazardRuleRegistry registry;
    private final WindLoadingEvaluator windLoadingEvaluator;
    private final SpatialQueryService spatialQueryService;
    private final SpatialProperties spatialProperties;

    public RouteHazardEngine(RouteRepository routeRepository,
                             ReadingRepository readingRepository,
                             BulletinRepository bulletinRepository,
                             HazardRuleRegistry registry,
                             WindLoadingEvaluator windLoadingEvaluator,
                             SpatialQueryService spatialQueryService,
                             SpatialProperties spatialProperties) {
        this.routeRepository = routeRepository;
        this.readingRepository = readingRepository;
        this.bulletinRepository = bulletinRepository;
        this.registry = registry;
        this.windLoadingEvaluator = windLoadingEvaluator;
        this.spatialQueryService = spatialQueryService;
        this.spatialProperties = spatialProperties;
    }

    public Optional<SkiRoute> route(String slug) {
        return routeRepository.findBySlug(slug);
    }

    @Cacheable(cacheNames = "hazard", key = "#slug + ':' + #windowHours")
    public RouteHazardAssessment assess(String slug, int windowHours) {
        SkiRoute route = routeRepository.findBySlug(slug)
                .orElseThrow(() -> new RouteNotFoundException(slug));
        return assess(route, windowHours);
    }

    public RouteHazardAssessment assess(SkiRoute route, int windowHours) {
        Instant reference = readingRepository.latestTimestamp().orElse(Instant.now());
        Instant windowStart = reference.minus(Duration.ofHours(windowHours));
        Instant snowWindowStart = reference.minus(Duration.ofHours(24));
        Map<String, ReadingAggregate> aggregates = readingRepository.aggregate(windowStart, snowWindowStart);
        Map<String, Integer> ratings = bulletinRepository.latestRatings();

        List<SegmentHazard> segments = new ArrayList<>();
        List<RoutePoint> points = route.points();
        for (int i = 0; i < points.size() - 1; i++) {
            segments.add(evaluateSegment(i, points.get(i), points.get(i + 1), route.region(), aggregates, ratings));
        }

        double maxScore = segments.stream().mapToDouble(SegmentHazard::score).max().orElse(0.0);
        double meanScore = segments.stream().mapToDouble(SegmentHazard::score).average().orElse(0.0);
        double maxSlope = segments.stream().mapToDouble(SegmentHazard::slopeDeg).max().orElse(0.0);
        int steep = (int) segments.stream().filter(segment -> segment.slopeDeg() >= 30.0).count();
        HazardSeverity severity = segments.stream()
                .map(SegmentHazard::severity)
                .max(Comparator.comparingInt(HazardSeverity::rank))
                .orElse(HazardSeverity.LOW);

        return new RouteHazardAssessment(route.slug(), route.name(), route.region(), reference, windowHours,
                GeoMath.round(maxScore, 2), GeoMath.round(meanScore, 2), severity, GeoMath.round(maxSlope, 2),
                steep, segments.size(), List.copyOf(segments));
    }

    private SegmentHazard evaluateSegment(int index, RoutePoint from, RoutePoint to, String region,
                                          Map<String, ReadingAggregate> aggregates, Map<String, Integer> ratings) {
        double midLat = (from.lat() + to.lat()) / 2.0;
        double midLon = (from.lon() + to.lon()) / 2.0;
        double elevation = (from.elevationM() + to.elevationM()) / 2.0;
        double horizontal = GeoMath.haversineM(from.lat(), from.lon(), to.lat(), to.lon());
        double slope = Math.abs(GeoMath.slopeDegrees(to.elevationM() - from.elevationM(), horizontal));
        // The fall line of a slope runs against the direction of travel on an ascent, so the
        // terrain aspect is the reverse of the track bearing (aspect wraps at 360 degrees).
        double aspect = GeoMath.normalizeDegrees(
                GeoMath.bearingDeg(from.lat(), from.lon(), to.lat(), to.lon()) + 180.0);

        ElevationBand band = ElevationBand.forElevation(elevation);
        HazardRuleStrategy rule = registry.forBand(band);

        Optional<StationProximity> nearest = spatialQueryService.nearest(midLat, midLon,
                spatialProperties.getProximityRadiusM());
        double gust = 0.0;
        double windDir = 0.0;
        double airTemp = 0.0;
        double snowDepth = 0.0;
        double newSnow = 0.0;
        double stationDistance = Double.MAX_VALUE;
        String stationId = null;
        String stationName = null;

        if (nearest.isPresent()) {
            StationProximity proximity = nearest.get();
            stationDistance = proximity.distanceM();
            stationId = proximity.station().stationId();
            stationName = proximity.station().name();
            ReadingAggregate aggregate = aggregates.get(stationId);
            if (aggregate != null) {
                gust = value(aggregate.latest().windGustMs());
                windDir = value(aggregate.latest().windDirDeg());
                airTemp = value(aggregate.latest().airTempC());
                snowDepth = value(aggregate.latest().snowDepthCm());
                newSnow = aggregate.newSnow24hCm();
            }
        }

        int dangerRating = ratings.getOrDefault(region + "|" + band.name(), 2);
        double leeAlignment = nearest.isPresent() ? windLoadingEvaluator.leeAlignment(aspect, windDir) : 0.0;

        HazardInput input = new HazardInput(slope, aspect, elevation, newSnow, snowDepth, gust, windDir, airTemp,
                dangerRating, leeAlignment, stationDistance);
        double score = rule.evaluate(input);

        return new SegmentHazard(index,
                GeoMath.round(from.lat(), 6), GeoMath.round(from.lon(), 6),
                GeoMath.round(to.lat(), 6), GeoMath.round(to.lon(), 6),
                GeoMath.round(elevation, 1), GeoMath.round(slope, 2), GeoMath.round(aspect, 1),
                GeoMath.round(score, 2), HazardSeverity.fromScore(score), band,
                stationId, stationName, GeoMath.round(stationDistance, 1),
                GeoMath.round(leeAlignment, 3), rule.flags(input));
    }

    private static double value(Double candidate) {
        return candidate == null ? 0.0 : candidate;
    }
}
