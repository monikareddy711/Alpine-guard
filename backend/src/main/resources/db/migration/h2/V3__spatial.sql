ALTER TABLE station ADD COLUMN geom GEOMETRY;
CREATE SPATIAL INDEX idx_station_geom ON station (geom);
