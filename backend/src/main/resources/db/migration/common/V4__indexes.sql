CREATE INDEX idx_reading_station_time ON station_reading (station_id, measured_at DESC);
CREATE INDEX idx_route_point_route ON route_point (route_id, seq);
CREATE INDEX idx_station_region ON station (region);
CREATE INDEX idx_station_elevation ON station (elevation_m);
CREATE INDEX idx_bulletin_region_date ON bulletin (region, valid_date);
