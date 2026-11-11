package io.alpineguard.benchmark;

import java.time.Instant;
import java.util.List;

public record BenchmarkReport(
        Instant executedAt,
        String engine,
        int stations,
        int segments,
        double radiusM,
        List<StrategyStats> strategies) {

    public record StrategyStats(
            String strategy,
            int samples,
            double p50Ms,
            double p95Ms,
            double p99Ms,
            double meanMs,
            double averageMatches,
            long heapBytes,
            long coldStartMs) {
    }
}
