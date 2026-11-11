package io.alpineguard.repository;

import io.alpineguard.domain.Station;
import io.alpineguard.spatial.SpatialDialect;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StationRepository {

    private static final RowMapper<Station> MAPPER = (rs, rowNum) -> new Station(
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

    public StationRepository(JdbcTemplate jdbcTemplate, SpatialDialect dialect) {
        this.jdbcTemplate = jdbcTemplate;
        this.dialect = dialect;
    }

    public List<Station> findAllActive() {
        return jdbcTemplate.query("SELECT * FROM station WHERE active = TRUE ORDER BY station_id", MAPPER);
    }

    public List<Station> findAll() {
        return jdbcTemplate.query("SELECT * FROM station ORDER BY station_id", MAPPER);
    }

    public Optional<Station> findByStationId(String stationId) {
        List<Station> rows = jdbcTemplate.query(
                "SELECT * FROM station WHERE station_id = ?", MAPPER, stationId);
        return rows.stream().findFirst();
    }

    public List<Station> findByRegion(String region) {
        return jdbcTemplate.query("SELECT * FROM station WHERE region = ? ORDER BY station_id", MAPPER, region);
    }

    public int count() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM station", Integer.class);
        return count == null ? 0 : count;
    }

    public void insert(Station station) {
        jdbcTemplate.update("INSERT INTO station (station_id, name, region, operator, network, elevation_m, lat, lon, active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                station.stationId(), station.name(), station.region(), station.operator(), station.network(),
                station.elevationM(), station.lat(), station.lon(), station.active());
    }

    public void insertAll(List<Station> stations) {
        jdbcTemplate.batchUpdate("INSERT INTO station (station_id, name, region, operator, network, elevation_m, lat, lon, active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                stations.stream().map(s -> new Object[]{s.stationId(), s.name(), s.region(), s.operator(), s.network(),
                        s.elevationM(), s.lat(), s.lon(), s.active()}).toList());
    }

    public void backfillGeometry() {
        jdbcTemplate.update(dialect.backfillGeometrySql());
    }
}
