package io.alpineguard.api;

import io.alpineguard.api.dto.GeoJsonFeatureCollectionBuilder;
import io.alpineguard.api.dto.HazardAssessmentDto;
import io.alpineguard.api.dto.RouteDetailDto;
import io.alpineguard.api.dto.RoutePointDto;
import io.alpineguard.api.dto.RouteSummaryDto;
import io.alpineguard.domain.HazardSeverity;
import io.alpineguard.domain.RouteHazardAssessment;
import io.alpineguard.domain.RoutePoint;
import io.alpineguard.domain.SegmentHazard;
import io.alpineguard.domain.SkiRoute;
import io.alpineguard.hazard.RouteHazardEngine;
import io.alpineguard.hazard.RouteNotFoundException;
import io.alpineguard.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteHazardEngine hazardEngine;

    public RouteService(RouteRepository routeRepository, RouteHazardEngine hazardEngine) {
        this.routeRepository = routeRepository;
        this.hazardEngine = hazardEngine;
    }

    public List<RouteSummaryDto> summaries(int windowHours) {
        List<RouteSummaryDto> summaries = new ArrayList<>();
        for (SkiRoute summary : routeRepository.findAll()) {
            SkiRoute route = routeRepository.findBySlug(summary.slug()).orElse(summary);
            RouteHazardAssessment assessment = hazardEngine.assess(route, windowHours);
            summaries.add(new RouteSummaryDto(route.slug(), route.name(), route.region(), route.difficulty(),
                    route.distanceM(), route.ascentM(), route.descentM(), route.maxElevationM(),
                    route.minElevationM(), assessment.maxScore(), assessment.meanScore(), assessment.severity(),
                    assessment.maxSlopeDeg(), assessment.steepSegmentCount(), assessment.totalSegments()));
        }
        return summaries;
    }

    public RouteDetailDto detail(String slug) {
        SkiRoute route = routeRepository.findBySlug(slug).orElseThrow(() -> new RouteNotFoundException(slug));
        List<List<Double>> coordinates = new ArrayList<>();
        for (RoutePoint point : route.points()) {
            coordinates.add(List.of(point.lon(), point.lat(), point.elevationM()));
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("slug", route.slug());
        properties.put("name", route.name());
        properties.put("region", route.region());
        properties.put("difficulty", route.difficulty());
        Map<String, Object> geoJson = GeoJsonFeatureCollectionBuilder.create()
                .line(coordinates, properties)
                .build();
        List<RoutePointDto> points = route.points().stream().map(RoutePointDto::from).toList();
        return new RouteDetailDto(route.slug(), route.name(), route.region(), route.difficulty(), route.distanceM(),
                route.ascentM(), route.descentM(), route.maxElevationM(), route.minElevationM(), points, geoJson);
    }

    public HazardAssessmentDto assessment(String slug, int windowHours) {
        RouteHazardAssessment assessment = hazardEngine.assess(slug, windowHours);
        return HazardAssessmentDto.of(assessment, segmentGeoJson(assessment));
    }

    private Map<String, Object> segmentGeoJson(RouteHazardAssessment assessment) {
        GeoJsonFeatureCollectionBuilder builder = GeoJsonFeatureCollectionBuilder.create();
        for (SegmentHazard segment : assessment.segments()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("index", segment.index());
            properties.put("score", segment.score());
            properties.put("severity", segment.severity().name());
            properties.put("slopeDeg", segment.slopeDeg());
            properties.put("aspectDeg", segment.aspectDeg());
            properties.put("elevationM", segment.elevationM());
            properties.put("band", segment.band().name());
            properties.put("nearestStationId", segment.nearestStationId());
            properties.put("flags", segment.flags());
            builder.line(List.of(
                    List.of(segment.startLon(), segment.startLat(), segment.elevationM()),
                    List.of(segment.endLon(), segment.endLat(), segment.elevationM())), properties);
        }
        return builder.build();
    }

    public HazardSeverity routeSeverity(String slug) {
        return hazardEngine.assess(slug, RouteHazardEngine.DEFAULT_WINDOW_HOURS).severity();
    }
}
