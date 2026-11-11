package io.alpineguard.spatial;

public class PostgisDialect implements SpatialDialect {

    @Override
    public String name() {
        return "postgis";
    }

    @Override
    public String withinSql() {
        return "SELECT s.id, s.station_id, s.name, s.region, s.operator, s.network, s.elevation_m, s.lat, s.lon, s.active "
                + "FROM station s WHERE s.active = TRUE AND ST_DWithin(s.geom::geography, "
                + "ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?)";
    }

    @Override
    public double radiusParameter(double radiusM, double lat) {
        return radiusM;
    }

    @Override
    public String backfillGeometrySql() {
        return "UPDATE station SET geom = ST_SetSRID(ST_MakePoint(lon, lat), 4326) WHERE geom IS NULL";
    }
}
