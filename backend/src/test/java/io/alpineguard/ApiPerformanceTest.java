package io.alpineguard;

import io.alpineguard.api.RouteService;
import io.alpineguard.api.StationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** Cached payload latency target: p95 under 120 ms for telemetry and hazard assessments. */
class ApiPerformanceTest extends AbstractIntegrationTest {

    private static final int SAMPLES = 60;

    @Autowired
    private StationService stationService;

    @Autowired
    private RouteService routeService;

    @Test
    void cachedTelemetryAnswersWithinBudget() {
        stationService.telemetry("AT07-103", 72);
        double[] latencies = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long started = System.nanoTime();
            stationService.telemetry("AT07-103", 72);
            latencies[i] = (System.nanoTime() - started) / 1_000_000.0;
        }
        assertThat(percentile(latencies, 95)).isLessThan(120.0);
    }

    @Test
    void cachedAssessmentAnswersWithinBudget() {
        routeService.assessment("stubai-gletscher-schaufelspitze", 72);
        double[] latencies = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long started = System.nanoTime();
            routeService.assessment("stubai-gletscher-schaufelspitze", 72);
            latencies[i] = (System.nanoTime() - started) / 1_000_000.0;
        }
        assertThat(percentile(latencies, 95)).isLessThan(120.0);
    }

    private double percentile(double[] values, int pct) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int index = (int) Math.ceil(pct / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }
}
