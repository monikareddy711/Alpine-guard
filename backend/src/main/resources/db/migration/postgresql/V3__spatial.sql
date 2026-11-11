CREATE EXTENSION IF NOT EXISTS postgis;
ALTER TABLE station ADD COLUMN geom geometry(Point, 4326);
CREATE INDEX idx_station_geom ON station USING GIST (geom);
