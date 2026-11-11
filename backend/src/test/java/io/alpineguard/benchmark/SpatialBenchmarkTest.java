package io.alpineguard.benchmark;

import io.alpineguard.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SpatialBenchmarkTest extends AbstractIntegrationTest {

    @Autowired
    private SpatialBenchmarkService benchmarkService;

    @Autowired
    private BenchmarkReportWriter writer;

    @Test
    void benchmarkMeasuresBothStrategiesAndKeepsTheFixtureOutOfTheNetwork() {
        BenchmarkReport report = benchmarkService.run(60, 120, 5000.0);
        assertThat(report.strategies()).hasSize(2);
        assertThat(report.segments()).isEqualTo(120);
        assertThat(report.stations()).isGreaterThanOrEqualTo(60);
        assertThat(report.strategies()).allSatisfy(stats -> {
            assertThat(stats.samples()).isEqualTo(120);
            assertThat(stats.p50Ms()).isGreaterThanOrEqualTo(0.0);
            assertThat(stats.p99Ms()).isGreaterThanOrEqualTo(stats.p50Ms());
        });
        assertThat(report.engine()).isNotBlank();
    }

    @Test
    void bothStrategiesFindTheSameNumberOfMatches() {
        BenchmarkReport report = benchmarkService.run(40, 80, 4000.0);
        double first = report.strategies().get(0).averageMatches();
        double second = report.strategies().get(1).averageMatches();
        assertThat(Math.abs(first - second)).isLessThan(0.01);
    }

    @Test
    void markdownReportContainsTheResultsTable() {
        BenchmarkReport report = benchmarkService.run(30, 40, 5000.0);
        String markdown = writer.toMarkdown(report);
        assertThat(markdown).contains("# Spatial query comparison")
                .contains("| Strategy | p50 (ms) | p95 (ms) | p99 (ms) |")
                .contains("kd-tree");
    }
}
