# Architecture

## Component flow

1. `ingest` reads the bundled EAWS / Albina station GeoJSON, the hourly SMET style reading CSV,
   the CAAML shaped bulletin JSON and the GPX tracks, normalizes them and writes the station,
   reading, bulletin and route tables.
2. `repository` exposes JDBC access to those tables. `StationIndex` keeps an immutable snapshot
   of the active stations and a k-d tree built from their coordinates.
3. `hazard.RouteHazardEngine` walks the route points, resolves the nearest station through the
   configured `spatial` strategy, and scores every segment with the rule set of its elevation
   band (Strategy pattern: `LowBandHazardRule`, `MidBandHazardRule`, `HighBandHazardRule`).
4. `api` wraps the assessment in DTOs and a GeoJSON feature collection (Builder pattern:
   `GeoJsonFeatureCollectionBuilder`) and serves it over REST with OpenAPI annotations.
5. The Angular `DashboardStore` holds the selection state as RxJS subjects. The PrimeNG tables,
   the Plotly charts and the Cesium viewer subscribe to the same streams, so a selection made in
   one panel is visible in the others in the same tick.

## Data model

| Table | Contents |
| --- | --- |
| `station` | Station metadata plus a PostGIS point (or H2GIS geometry in the development profile) |
| `station_reading` | Hourly temperature, wind, gust, direction, snow depth and new snow per station |
| `ski_route` / `route_point` | Tour geometry with distance, slope angle and aspect per point |
| `bulletin` | Danger rating per region and elevation band, one row per band |

## API summary

| Endpoint | Returns |
| --- | --- |
| `GET /api/v1/stations` | Station list with the newest reading; filters `region`, `minElevation`, `maxElevation`, `band`, `minDanger` |
| `GET /api/v1/stations/geojson` | Stations as a GeoJSON feature collection |
| `GET /api/v1/stations/{id}/telemetry?hours=72` | Hourly telemetry arrays for the requested window |
| `GET /api/v1/routes` | Tour summaries with the aggregated hazard score |
| `GET /api/v1/routes/{slug}` | Route geometry with per point slope and aspect |
| `GET /api/v1/routes/{slug}/hazard-assessment?windowHours=72` | Segment breakdown plus a GeoJSON line per segment |
| `GET /api/v1/bulletins` | Newest bulletins per region and band |
| `GET /api/v1/benchmark` | Proximity benchmark percentiles for both strategies |
| `POST /api/v1/ingest/refresh?force=true` | Re-runs the bundled feed ingestion |

## Terrain pipeline

`tools/build_terrain.py` mosaics the Copernicus GLO-30 tiles over 9-12 E, 46-47.5 N and emits two
asset sets under `frontend/public/terrain/`: 16 bit heightmap buffers in three quadtree levels,
consumed by a custom `TerrainProvider`, and hillshade imagery tiles tinted by elevation.
`tools/build_dataset.py` derives the GPX tracks as least cost paths over the same DEM, so the
recorded slope angles follow real terrain rather than straight lines.
