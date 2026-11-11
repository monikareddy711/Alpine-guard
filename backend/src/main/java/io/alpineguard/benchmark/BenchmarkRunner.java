package io.alpineguard.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes docs/spatial-query-comparison.md when the service is started with the report flag. */
@Component
public class BenchmarkRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkRunner.class);

    private final SpatialBenchmarkService benchmarkService;
    private final BenchmarkReportWriter writer;
    private final boolean enabled;
    private final String reportPath;

    public BenchmarkRunner(SpatialBenchmarkService benchmarkService, BenchmarkReportWriter writer,
                           @Value("${alpineguard.benchmark.run-on-startup:false}") boolean enabled,
                           @Value("${alpineguard.benchmark.report-path:docs/spatial-query-comparison.md}") String reportPath) {
        this.benchmarkService = benchmarkService;
        this.writer = writer;
        this.enabled = enabled;
        this.reportPath = reportPath;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled) {
            return;
        }
        BenchmarkReport report = benchmarkService.run(benchmarkService.defaultStationCount(), 1000,
                benchmarkService.defaultRadius());
        Path path = Path.of(reportPath);
        Files.createDirectories(path.toAbsolutePath().getParent());
        Files.writeString(path, writer.toMarkdown(report));
        LOG.info("benchmark report written to {}", path.toAbsolutePath());
    }
}
