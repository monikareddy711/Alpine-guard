package io.alpineguard.api;

import io.alpineguard.benchmark.BenchmarkReport;
import io.alpineguard.benchmark.SpatialBenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/benchmark")
@Tag(name = "benchmark", description = "PostGIS versus in-memory proximity comparison")
public class BenchmarkController {

    private final SpatialBenchmarkService benchmarkService;

    public BenchmarkController(SpatialBenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @GetMapping
    @Operation(summary = "Run the proximity benchmark and return latency percentiles")
    public BenchmarkReport run(@RequestParam(defaultValue = "250") int stations,
                               @RequestParam(defaultValue = "1000") int segments) {
        if (stations < 10 || stations > 5000) {
            throw new IllegalArgumentException("stations must be between 10 and 5000");
        }
        if (segments < 10 || segments > 20000) {
            throw new IllegalArgumentException("segments must be between 10 and 20000");
        }
        return benchmarkService.run(stations, segments, benchmarkService.defaultRadius());
    }
}
