package io.alpineguard.spatial;

import io.alpineguard.domain.GeoMath;
import io.alpineguard.domain.Station;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Radius filter executed inside the database against the spatial index on station.geom. */
@Component
public class DatabaseSpatialStrategy implements SpatialQueryStrategy {

    private static final RowMapper<Station> STATION_MAPPER = (rs, rowNum) -> new Station(
            rs.getLong("id"),
            rs.getString("station_id"),
            rs.getString("name"),
            rs.getString("region"),
            rs.getString("operator"),
            rs.getString("network"),
            rs.getDouble("elevation_m"),
            rs.getDouble("lat"),
            rs.getDouble("lon"),
            rs.getBoolean("active"));

    private final JdbcTemplate jdbcTemplate;
    private final SpatialDialect dialect;

    public DatabaseSpatialStrategy(JdbcTemplate jdbcTemplate, SpatialDialect dialect) {
        this.jdbcTemplate = jdbcTemplate;
        this.dialect = dialect;
    }

    @Override
    public String name() {
        return dialect.name() + "-dwithin";
    }

    @Override
    public List<StationProximity> within(double lat, double lon, double radiusM) {
        double radius = dialect.radiusParameter(radiusM, lat);
        List<Station> rows = jdbcTemplate.query(dialect.withinSql(), STATION_MAPPER, lon, lat, radius);
        List<StationProximity> hits = new ArrayList<>(rows.size());
        for (Station station : rows) {
            double distance = GeoMath.haversineM(lat, lon, station.lat(), station.lon());
            if (distance <= radiusM) {
                hits.add(new StationProximity(station, distance));
            }
        }
        hits.sort(Comparator.comparingDouble(StationProximity::distanceM));
        return hits;
    }

    @Override
    public Optional<StationProximity> nearest(double lat, double lon, double radiusM) {
        return within(lat, lon, radiusM).stream().findFirst();
    }
}
