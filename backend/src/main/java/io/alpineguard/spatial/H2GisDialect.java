package io.alpineguard.spatial;

public class H2GisDialect implements SpatialDialect {

    private static final double METERS_PER_DEGREE = 111320.0;

    @Override
    public String name() {
        return "h2gis";
    }

    @Override
    public String withinSql() {
        return "SELECT s.id, s.station_id, s.name, s.region, s.operator, s.network, s.elevation_m, s.lat, s.lon, s.active "
                + "FROM station s WHERE s.active = TRUE AND ST_DWithin(s.geom, "
                + "ST_SetSRID(ST_MakePoint(?, ?), 4326), ?)";
    }

    @Override
    public double radiusParameter(double radiusM, double lat) {
        // H2GIS filters on a planar geometry in degrees, and a degree of longitude is only
        // cos(latitude) of a degree of latitude, so the degree radius is widened by 1/cos(lat)
        // to keep the index prefilter a superset of the metric radius. The exact haversine cut
        // runs after the rows come back.
        return radiusM / (METERS_PER_DEGREE * Math.cos(Math.toRadians(lat)));
    }

    @Override
    public String backfillGeometrySql() {
        return "UPDATE station SET geom = ST_SetSRID(ST_MakePoint(lon, lat), 4326) WHERE geom IS NULL";
    }
}
