package io.alpineguard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void stationListReturnsTheNetwork() throws Exception {
        mockMvc.perform(get("/api/v1/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(24)))
                .andExpect(jsonPath("$[0].stationId").exists())
                .andExpect(jsonPath("$[0].elevationBand").exists());
    }

    @Test
    void stationListFiltersByElevationAndBand() throws Exception {
        mockMvc.perform(get("/api/v1/stations").param("minElevation", "2500").param("band", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].elevationM")
                        .value(org.hamcrest.Matchers.everyItem(greaterThanOrEqualTo(2800.0))));
        mockMvc.perform(get("/api/v1/stations").param("region", "CH-GR Graubuenden"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)));
    }

    @Test
    void stationTelemetryReturns72HourSeries() throws Exception {
        mockMvc.perform(get("/api/v1/stations/AT07-103/telemetry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("AT07-103"))
                .andExpect(jsonPath("$.hours").value(72))
                .andExpect(jsonPath("$.timestamps.length()", greaterThan(60)))
                .andExpect(jsonPath("$.snowDepthCm.length()", greaterThan(60)));
    }

    @Test
    void telemetryRejectsImpossibleWindow() throws Exception {
        mockMvc.perform(get("/api/v1/stations/AT07-103/telemetry").param("hours", "5000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stationGeojsonHasFeatures() throws Exception {
        mockMvc.perform(get("/api/v1/stations/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features", hasSize(24)));
    }

    @Test
    void unknownStationReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/stations/XX-000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void routeListCarriesHazardSummaries() throws Exception {
        mockMvc.perform(get("/api/v1/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].severity").exists())
                .andExpect(jsonPath("$[0].maxScore").value(greaterThan(0.0)));
    }

    @Test
    void routeDetailReturnsGeometry() throws Exception {
        mockMvc.perform(get("/api/v1/routes/sulzkogel-kuehtai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()", greaterThan(20)))
                .andExpect(jsonPath("$.geoJson.features[0].geometry.type").value("LineString"));
    }

    @Test
    void hazardAssessmentReturnsSegmentsAndGeojson() throws Exception {
        mockMvc.perform(get("/api/v1/routes/pitztal-wildspitze/hazard-assessment").param("windowHours", "72"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeSlug").value("pitztal-wildspitze"))
                .andExpect(jsonPath("$.segments.length()", greaterThan(20)))
                .andExpect(jsonPath("$.geoJson.features.length()", greaterThan(20)))
                .andExpect(jsonPath("$.severity").exists());
    }

    @Test
    void defaultAssessmentEndpointMatchesTheExplicitWindow() throws Exception {
        mockMvc.perform(get("/api/v1/routes/sulzkogel-kuehtai/hazard-assessment/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowHours").value(72));
    }

    @Test
    void missingRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/routes/not-a-route/hazard-assessment"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulletinsListLatestPerBand() throws Exception {
        mockMvc.perform(get("/api/v1/bulletins").param("region", "AT-07 Tyrol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
        mockMvc.perform(get("/api/v1/bulletins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void ingestRefreshRefusesToDuplicateTheSeed() throws Exception {
        mockMvc.perform(post("/api/v1/ingest/refresh"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openApiDocumentIsPublished() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("AlpineGuard API"));
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
