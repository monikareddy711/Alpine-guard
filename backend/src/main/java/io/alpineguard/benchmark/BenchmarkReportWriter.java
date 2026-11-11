package io.alpineguard.benchmark;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BenchmarkReportWriter {

    public String toMarkdown(BenchmarkReport report) {
        StringBuilder md = new StringBuilder();
        md.append("# Spatial query comparison\n\n");
        md.append("Both strategies answer the same question: which weather stations sit within the\n")
                .append("proximity radius of a route segment midpoint. Strategy A lets the database spatial\n")
                .append("index do the filtering, strategy B keeps the station coordinates in a JVM k-d tree.\n\n");

        md.append("## Method\n\n");
        md.append("- Fixture: ").append(report.stations())
                .append(" stations (the seeded network plus deterministic synthetic rows), ")
                .append(report.segments()).append(" segment midpoints sampled along the five ski routes.\n");
        md.append(String.format(Locale.ROOT, "- Radius: %.0f m. Warm-up: 3 full passes per strategy before timing.%n",
                report.radiusM()));
        md.append("- Latency is measured per query on the JVM side, so the database strategy includes the\n")
                .append("  JDBC round trip. Percentiles are taken over the sorted per-query samples.\n");
        md.append("- Heap is the JVM heap retained after the first query of a strategy (post GC).\n");
        md.append("- Cold start is the k-d tree build time for strategy B and the first statement\n")
                .append("  execution for strategy A.\n");
        md.append("- Engine used for this run: ").append(report.engine()).append(".\n");
        md.append("- The harness measures whatever JDBC URL is configured. The bundled development\n")
                .append("  profile runs on H2 with H2GIS; `docker compose` points ALPINEGUARD_DB_URL at\n")
                .append("  PostgreSQL 16 with PostGIS, and the same command then measures the ST_DWithin\n")
                .append("  path against the GIST index.\n\n");

        md.append("## Results\n\n");
        md.append("| Strategy | p50 (ms) | p95 (ms) | p99 (ms) | mean (ms) | avg matches | heap (KB) | cold start (ms) |\n");
        md.append("| --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (BenchmarkReport.StrategyStats stats : report.strategies()) {
            md.append(String.format(Locale.ROOT, "| %s | %.3f | %.3f | %.3f | %.3f | %.1f | %d | %d |%n",
                    stats.strategy(), stats.p50Ms(), stats.p95Ms(), stats.p99Ms(), stats.meanMs(),
                    stats.averageMatches(), stats.heapBytes() / 1024, stats.coldStartMs()));
        }
        md.append('\n');
        md.append("## Reading\n\n");
        md.append("The in-memory tree answers a radius query in microseconds once built, and the build\n")
                .append("over a few hundred stations is short enough to run at startup. The database strategy\n")
                .append("pays a round trip per query but needs no warm-up and stays correct when stations are\n")
                .append("edited while the service is running. The default configuration therefore uses the\n")
                .append("k-d tree for per-segment hazard scoring and keeps the PostGIS path for bulk and\n")
                .append("ad-hoc spatial queries, where a single statement replaces many JVM queries.\n\n");
        md.append("Reproduce with:\n\n");
        md.append("```bash\n")
                .append("curl -s http://localhost:8080/api/v1/benchmark | jq\n")
                .append("```\n");
        return md.toString();
    }
}
