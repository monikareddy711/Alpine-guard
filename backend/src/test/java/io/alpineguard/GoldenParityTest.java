package io.alpineguard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.alpineguard.domain.RouteHazardAssessment;
import io.alpineguard.domain.RoutePoint;
import io.alpineguard.domain.SegmentHazard;
import io.alpineguard.domain.SkiRoute;
import io.alpineguard.domain.Station;
import io.alpineguard.hazard.RouteHazardEngine;
import io.alpineguard.ingest.GpxRouteParser;
import io.alpineguard.repository.StationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deterministic parity check against the pinned storm snapshot: ten station snapshots and
 * three GPX tracks whose hazard scores, maximum slope angles and station distances were
 * recorded from a verified engine run.
 */
class GoldenParityTest extends AbstractIntegrationTest {

    private static final int WINDOW_HOURS = 72;
    private static final Path GOLDEN_PATH = Path.of("src/test/resources/golden/tyrol_davos_2026_storm.json");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RouteHazardEngine engine;

    @Autowired
    private GpxRouteParser gpxRouteParser;

    @Autowired
    private StationRepository stationRepository;

    @Test
    void stationSnapshotsMatchTheSeededNetwork() throws Exception {
        JsonNode golden = readGolden();
        assertThat(golden.get("stations")).hasSize(10);
        for (JsonNode snapshot : golden.get("stations")) {
            Station station = stationRepository.findByStationId(snapshot.get("stationId").asText()).orElseThrow();
            assertThat(station.name()).isEqualTo(snapshot.get("name").asText());
            assertThat(station.elevationM()).isEqualTo(snapshot.get("elevationM").asDouble());
            assertThat(station.lat()).isEqualTo(snapshot.get("lat").asDouble());
            assertThat(station.lon()).isEqualTo(snapshot.get("lon").asDouble());
        }
    }

    @Test
    void hazardScoresMatchThePinnedExpectations() throws Exception {
        JsonNode golden = readGolden();
        ArrayNode computed = objectMapper.createArrayNode();
        for (JsonNode routeNode : golden.get("routes")) {
            String slug = routeNode.get("slug").asText();
            SkiRoute route = loadRoute(slug, routeNode.get("gpx").asText());
            RouteHazardAssessment assessment = engine.assess(route, WINDOW_HOURS);
            computed.add(describe(assessment));

            JsonNode expected = golden.path("expected").path("routes");
            if (expected.isMissingNode() || expected.isEmpty()) {
                continue;
            }
            JsonNode expectedRoute = findRoute(expected, slug);
            assertThat(assessment.maxScore()).isEqualTo(expectedRoute.get("maxScore").asDouble());
            assertThat(assessment.meanScore()).isEqualTo(expectedRoute.get("meanScore").asDouble());
            assertThat(assessment.severity().name()).isEqualTo(expectedRoute.get("severity").asText());
            assertThat(assessment.maxSlopeDeg()).isEqualTo(expectedRoute.get("maxSlopeDeg").asDouble());
            assertThat(assessment.steepSegmentCount()).isEqualTo(expectedRoute.get("steepSegmentCount").asInt());
            assertThat(assessment.totalSegments()).isEqualTo(expectedRoute.get("totalSegments").asInt());
            JsonNode expectedSegments = expectedRoute.get("segments");
            assertThat(assessment.segments()).hasSize(expectedSegments.size());
            for (int i = 0; i < expectedSegments.size(); i++) {
                SegmentHazard actual = assessment.segments().get(i);
                JsonNode expectedSegment = expectedSegments.get(i);
                assertThat(actual.index()).isEqualTo(expectedSegment.get("index").asInt());
                assertThat(actual.slopeDeg()).isEqualTo(expectedSegment.get("slopeDeg").asDouble());
                assertThat(actual.aspectDeg()).isEqualTo(expectedSegment.get("aspectDeg").asDouble());
                assertThat(actual.score()).isEqualTo(expectedSegment.get("score").asDouble());
                assertThat(actual.severity().name()).isEqualTo(expectedSegment.get("severity").asText());
                assertThat(actual.nearestStationId()).isEqualTo(expectedSegment.get("nearestStationId").asText());
                assertThat(actual.nearestStationDistanceM())
                        .isEqualTo(expectedSegment.get("nearestStationDistanceM").asDouble());
            }
        }
        if (Boolean.getBoolean("golden.write")) {
            writeExpectations(computed);
        }
    }

    private JsonNode findRoute(JsonNode routes, String slug) {
        for (JsonNode route : routes) {
            if (route.get("slug").asText().equals(slug)) {
                return route;
            }
        }
        throw new AssertionError("golden file has no expectation for " + slug);
    }

    private SkiRoute loadRoute(String slug, String resource) throws Exception {
        String region = slug.startsWith("weissfluhjoch") ? "CH-GR Graubuenden" : "AT-07 Tyrol";
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            List<RoutePoint> points = gpxRouteParser.buildRoutePoints(gpxRouteParser.readTrackPoints(in));
            return gpxRouteParser.toRoute(slug, slug, region, "D4", resource, points);
        }
    }

    private ObjectNode describe(RouteHazardAssessment assessment) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("slug", assessment.routeSlug());
        node.put("maxScore", assessment.maxScore());
        node.put("meanScore", assessment.meanScore());
        node.put("severity", assessment.severity().name());
        node.put("maxSlopeDeg", assessment.maxSlopeDeg());
        node.put("steepSegmentCount", assessment.steepSegmentCount());
        node.put("totalSegments", assessment.totalSegments());
        ArrayNode segments = objectMapper.createArrayNode();
        for (SegmentHazard segment : assessment.segments()) {
            ObjectNode segmentNode = objectMapper.createObjectNode();
            segmentNode.put("index", segment.index());
            segmentNode.put("slopeDeg", segment.slopeDeg());
            segmentNode.put("aspectDeg", segment.aspectDeg());
            segmentNode.put("score", segment.score());
            segmentNode.put("severity", segment.severity().name());
            segmentNode.put("nearestStationId", segment.nearestStationId());
            segmentNode.put("nearestStationDistanceM", segment.nearestStationDistanceM());
            segments.add(segmentNode);
        }
        node.set("segments", segments);
        return node;
    }

    private JsonNode readGolden() throws Exception {
        try (InputStream in = new ClassPathResource("golden/tyrol_davos_2026_storm.json").getInputStream()) {
            return objectMapper.readTree(in);
        }
    }

    private void writeExpectations(ArrayNode computedRoutes) throws Exception {
        ObjectNode root = (ObjectNode) readGolden();
        ObjectNode expected = objectMapper.createObjectNode();
        expected.put("windowHours", WINDOW_HOURS);
        expected.set("routes", computedRoutes);
        root.set("expected", expected);
        Files.writeString(GOLDEN_PATH, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n");
    }
}
