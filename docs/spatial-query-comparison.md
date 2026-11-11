# Spatial query comparison

Both strategies answer the same question: which weather stations sit within the
proximity radius of a route segment midpoint. Strategy A lets the database spatial
index do the filtering, strategy B keeps the station coordinates in a JVM k-d tree.

## Method

- Fixture: 250 stations (the seeded network plus deterministic synthetic rows), 1000 segment midpoints sampled along the five ski routes.
- Radius: 5000 m. Warm-up: 3 full passes per strategy before timing.
- Latency is measured per query on the JVM side, so the database strategy includes the
  JDBC round trip. Percentiles are taken over the sorted per-query samples.
- Heap is the JVM heap retained after the first query of a strategy (post GC).
- Cold start is the k-d tree build time for strategy B and the first statement
  execution for strategy A.
- Engine used for this run: H2 2.2.224 (2023-09-17).
- The harness measures whatever JDBC URL is configured. The bundled development
  profile runs on H2 with H2GIS; `docker compose` points ALPINEGUARD_DB_URL at
  PostgreSQL 16 with PostGIS, and the same command then measures the ST_DWithin
  path against the GIST index.

## Results

| Strategy | p50 (ms) | p95 (ms) | p99 (ms) | mean (ms) | avg matches | heap (KB) | cold start (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| h2gis-dwithin | 0.469 | 0.732 | 16.251 | 0.982 | 19.2 | 26 | 1 |
| kd-tree | 0.010 | 0.019 | 0.049 | 0.012 | 19.2 | 7 | 2 |

## Reading

The in-memory tree answers a radius query in microseconds once built, and the build
over a few hundred stations is short enough to run at startup. The database strategy
pays a round trip per query but needs no warm-up and stays correct when stations are
edited while the service is running. The default configuration therefore uses the
k-d tree for per-segment hazard scoring and keeps the PostGIS path for bulk and
ad-hoc spatial queries, where a single statement replaces many JVM queries.

Reproduce with:

```bash
curl -s http://localhost:8080/api/v1/benchmark | jq
```
