package io.alpineguard.benchmark;

import io.alpineguard.config.SpatialProperties;
import io.alpineguard.domain.Station;
import io.alpineguard.repository.RouteRepository;
import io.alpineguard.repository.StationRepository;
import io.alpineguard.spatial.SpatialQueryService;
import io.alpineguard.spatial.SpatialQueryStrategy;
import io.alpineguard.spatial.StationIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Repeatable comparison of the two proximity strategies. A benchmark fixture of synthetic
 * stations is written into the station table for the duration of the run (station ids carry
 * a BENCH prefix), the k-d tree is rebuilt over the same rows, and every strategy answers the
 * same segment queries so the measured latencies are comparable.
 */
@Service
public class SpatialBenchmarkService {

    private static final Logger LOG = LoggerFactory.getLogger(SpatialBenchmarkService.class);
    private static final int WARMUP_ROUNDS = 3;

    private final SpatialQueryService spatialQueryService;
    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final StationIndex stationIndex;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final SpatialProperties spatialProperties;

    public SpatialBenchmarkService(SpatialQueryService spatialQueryService, StationRepository stationRepository,
                                   RouteRepository routeRepository, StationIndex stationIndex,
                                   JdbcTemplate jdbcTemplate, DataSource dataSource,
                                   SpatialProperties spatialProperties) {
        this.spatialQueryService = spatialQueryService;
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.stationIndex = stationIndex;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.spatialProperties = spatialProperties;
    }

    public BenchmarkReport run(int stationCount, int segmentCount, double radiusM) {
        List<Station> fixture = buildFixture(stationCount);
        insertFixture(fixture);
        long coldStartMs = timeRefresh();
        try {
            double[][] segments = buildSegments(segmentCount);
            List<BenchmarkReport.StrategyStats> stats = new ArrayList<>();
            for (SpatialQueryStrategy strategy : orderedStrategies()) {
                stats.add(measure(strategy, segments, radiusM));
            }
            return new BenchmarkReport(Instant.now(), engineName(), fixture.size(), segmentCount, radiusM, stats);
        } finally {
            deleteFixture();
            stationIndex.refresh();
        }
    }

    private List<SpatialQueryStrategy> orderedStrategies() {
        List<SpatialQueryStrategy> strategies = new ArrayList<>(spatialQueryService.all().values());
        strategies.sort(Comparator.comparing(SpatialQueryStrategy::name));
        return strategies;
    }

    private long timeRefresh() {
        long started = System.nanoTime();
        stationIndex.refresh();
        return Math.round((System.nanoTime() - started) / 1_000_000.0);
    }

    private BenchmarkReport.StrategyStats measure(SpatialQueryStrategy strategy, double[][] segments, double radiusM) {
        for (int round = 0; round < WARMUP_ROUNDS; round++) {
            for (double[] segment : segments) {
                strategy.within(segment[0], segment[1], radiusM);
            }
        }
        long heapBefore = usedHeap();
        double[] latencies = new double[segments.length];
        long matches = 0;
        for (int i = 0; i < segments.length; i++) {
            long started = System.nanoTime();
            matches += strategy.within(segments[i][0], segments[i][1], radiusM).size();
            latencies[i] = (System.nanoTime() - started) / 1_000_000.0;
        }
        long heapAfter = usedHeap();
        Arrays.sort(latencies);
        double mean = Arrays.stream(latencies).average().orElse(0);
        long coldStart = strategy.name().startsWith("kd-tree") ? timeRefresh() : coldStartForDatabase();
        return new BenchmarkReport.StrategyStats(strategy.name(), segments.length,
                round(percentile(latencies, 50)), round(percentile(latencies, 95)),
                round(percentile(latencies, 99)), round(mean), round((double) matches / segments.length),
                Math.max(0, heapAfter - heapBefore), coldStart);
    }

    private long coldStartForDatabase() {
        long started = System.nanoTime();
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM station", Integer.class);
        return Math.round((System.nanoTime() - started) / 1_000_000.0);
    }

    private double percentile(double[] sorted, double pct) {
        if (sorted.length == 0) {
            return 0;
        }
        int index = (int) Math.min(sorted.length - 1, Math.round(pct / 100.0 * sorted.length) - 1);
        return sorted[Math.max(0, index)];
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private String engineName() {
        try (Connection connection = dataSource.getConnection()) {
            var meta = connection.getMetaData();
            return meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (SQLException e) {
            return "unknown";
        }
    }

    private List<Station> buildFixture(int stationCount) {
        List<Station> fixture = new ArrayList<>(stationRepository.findAllActive());
        SplitMix random = new SplitMix(42L);
        List<double[]> anchors = new ArrayList<>();
        for (var route : routeRepository.findAll()) {
            routeRepository.findBySlug(route.slug()).ifPresent(r ->
                    r.points().forEach(p -> anchors.add(new double[]{p.lat(), p.lon()})));
        }
        int index = 0;
        while (fixture.size() < stationCount && !anchors.isEmpty()) {
            double[] anchor = anchors.get(random.nextInt(anchors.size()));
            double[] offset = jitter(random, 0.12, 0.18);
            fixture.add(new Station(null, String.format("BENCH-%04d", ++index),
                    "Benchmark station " + index, index % 2 == 0 ? "AT-07 Tyrol" : "CH-GR Graubuenden",
                    "benchmark", "SYNTHETIC", 1500 + random.nextInt(1800),
                    anchor[0] + offset[0], anchor[1] + offset[1], true));
        }
        LOG.info("benchmark fixture holds {} stations", fixture.size());
        return fixture;
    }

    private void insertFixture(List<Station> fixture) {
        List<Station> synthetic = fixture.stream().filter(s -> s.stationId().startsWith("BENCH-")).toList();
        stationRepository.insertAll(synthetic);
        stationRepository.backfillGeometry();
    }

    private void deleteFixture() {
        jdbcTemplate.update("DELETE FROM station WHERE station_id LIKE 'BENCH-%'");
    }

    private double[][] buildSegments(int segmentCount) {
        List<double[]> anchors = new ArrayList<>();
        for (var route : routeRepository.findAll()) {
            routeRepository.findBySlug(route.slug()).ifPresent(r ->
                    r.points().forEach(p -> anchors.add(new double[]{p.lat(), p.lon()})));
        }
        SplitMix random = new SplitMix(7L);
        double[][] segments = new double[segmentCount][2];
        for (int i = 0; i < segmentCount; i++) {
            double[] anchor = anchors.get(random.nextInt(anchors.size()));
            double[] offset = jitter(random, 0.1, 0.15);
            segments[i][0] = anchor[0] + offset[0];
            segments[i][1] = anchor[1] + offset[1];
        }
        return segments;
    }

    private static double[] jitter(SplitMix random, double latSpanDeg, double lonSpanDeg) {
        return new double[]{
                (random.nextDouble() - 0.5) * latSpanDeg,
                (random.nextDouble() - 0.5) * lonSpanDeg
        };
    }

    /** Small deterministic generator so a benchmark run can be replayed exactly. */
    static final class SplitMix {
        private long state;

        SplitMix(long seed) {
            this.state = seed;
        }

        private long next() {
            state += 0x9E3779B97F4A7C15L;
            long z = state;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }

        double nextDouble() {
            return (next() >>> 11) * 0x1.0p-53;
        }

        int nextInt(int bound) {
            return (int) Math.floorMod(next(), (long) bound);
        }
    }

    public double defaultRadius() {
        return spatialProperties.getProximityRadiusM();
    }

    public int defaultStationCount() {
        return spatialProperties.getStationDatasetSize();
    }
}
