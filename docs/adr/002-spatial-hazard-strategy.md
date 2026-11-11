# 002 Spatial indexing for segment scoring

## Context
Each route segment needs the nearest station within 5 km, and a tour carries 25 to 60 segments.

## Decision
`ST_DWithin` on a PostGIS geography column with a GIST index and an in-memory k-d tree were both
benchmarked. The k-d tree is the default for per-segment scoring, the database strategy stays
selectable through `ALPINEGUARD_SPATIAL_STRATEGY`, and both sit behind `SpatialQueryStrategy`.

## Consequence
The tree answers in about 0.01 ms against 0.47 ms for the indexed query including the round trip, and
both return identical station sets. The tree only matches the loaded snapshot, so station edits need
`StationIndex.refresh()`; the database path needs no such step.
