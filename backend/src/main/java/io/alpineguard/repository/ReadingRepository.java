package io.alpineguard.repository;

import io.alpineguard.domain.StationReading;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ReadingRepository {

    private static final RowMapper<StationReading> MAPPER = (rs, rowNum) -> new StationReading(
            rs.getString("station_id"),
            rs.getTimestamp("measured_at").toInstant(),
            getDouble(rs, "air_temp_c"),
            getDouble(rs, "wind_speed_ms"),
            getDouble(rs, "wind_gust_ms"),
            getDouble(rs, "wind_dir_deg"),
            getDouble(rs, "snow_depth_cm"),
            getDouble(rs, "new_snow_cm"),
            getDouble(rs, "humidity_pct"),
            getDouble(rs, "pressure_hpa"));

    private final JdbcTemplate jdbcTemplate;

    public ReadingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static Double getDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    public void insertAll(List<StationReading> readings) {
        jdbcTemplate.batchUpdate("INSERT INTO station_reading (station_id, measured_at, air_temp_c, wind_speed_ms, "
                        + "wind_gust_ms, wind_dir_deg, snow_depth_cm, new_snow_cm, humidity_pct, pressure_hpa) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                readings.stream().map(r -> new Object[]{r.stationId(), Timestamp.from(r.measuredAt()), r.airTempC(),
                        r.windSpeedMs(), r.windGustMs(), r.windDirDeg(), r.snowDepthCm(), r.newSnowCm(),
                        r.humidityPct(), r.pressureHpa()}).toList());
    }

    public List<StationReading> series(String stationId, Instant from, Instant to) {
        return jdbcTemplate.query("SELECT * FROM station_reading WHERE station_id = ? AND measured_at >= ? "
                + "AND measured_at <= ? ORDER BY measured_at", MAPPER, stationId,
                Timestamp.from(from), Timestamp.from(to));
    }

    public Optional<Instant> latestTimestamp() {
        Timestamp ts = jdbcTemplate.queryForObject("SELECT MAX(measured_at) FROM station_reading", Timestamp.class);
        return Optional.ofNullable(ts).map(Timestamp::toInstant);
    }

    public int count() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM station_reading", Integer.class);
        return count == null ? 0 : count;
    }

    /**
     * One pass over the readings to collect the newest observation per station inside the
     * window plus the new snow accumulated since the snow window start. Doing this in two
     * statements keeps a route assessment at a single round trip instead of one per segment.
     */
    public Map<String, ReadingAggregate> aggregate(Instant windowStart, Instant snowWindowStart) {
        Map<String, StationReading> latest = new HashMap<>();
        jdbcTemplate.query("SELECT r.* FROM station_reading r JOIN (SELECT station_id, MAX(measured_at) AS newest "
                        + "FROM station_reading WHERE measured_at >= ? GROUP BY station_id) x "
                        + "ON r.station_id = x.station_id AND r.measured_at = x.newest",
                rs -> {
                    StationReading reading = MAPPER.mapRow(rs, 0);
                    latest.put(reading.stationId(), reading);
                }, Timestamp.from(windowStart));

        Map<String, Double> snow = new HashMap<>();
        jdbcTemplate.query("SELECT station_id, SUM(new_snow_cm) AS total FROM station_reading "
                        + "WHERE measured_at >= ? GROUP BY station_id",
                rs -> {
                    snow.put(rs.getString("station_id"), rs.getDouble("total"));
                }, Timestamp.from(snowWindowStart));

        Map<String, ReadingAggregate> result = new HashMap<>();
        latest.forEach((stationId, reading) ->
                result.put(stationId, new ReadingAggregate(reading, snow.getOrDefault(stationId, 0.0))));
        return result;
    }
}
