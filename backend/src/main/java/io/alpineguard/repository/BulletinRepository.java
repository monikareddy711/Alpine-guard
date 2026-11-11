package io.alpineguard.repository;

import io.alpineguard.domain.AvalancheBulletin;
import io.alpineguard.domain.ElevationBand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class BulletinRepository {

    private static final RowMapper<AvalancheBulletin> MAPPER = (rs, rowNum) -> new AvalancheBulletin(
            rs.getString("region"),
            rs.getDate("valid_date").toLocalDate(),
            ElevationBand.valueOf(rs.getString("elevation_band")),
            rs.getInt("danger_rating"),
            rs.getString("main_problem"),
            rs.getString("summary"),
            rs.getTimestamp("issued_at").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public BulletinRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertAll(List<AvalancheBulletin> bulletins) {
        jdbcTemplate.batchUpdate("INSERT INTO bulletin (region, valid_date, elevation_band, danger_rating, "
                        + "main_problem, summary, issued_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                bulletins.stream().map(b -> new Object[]{b.region(), Date.valueOf(b.validDate()),
                        b.elevationBand().name(), b.dangerRating(), b.mainProblem(), b.summary(),
                        Timestamp.from(b.issuedAt())}).toList());
    }

    public List<AvalancheBulletin> findByRegionAndDate(String region, LocalDate date) {
        return jdbcTemplate.query("SELECT * FROM bulletin WHERE region = ? AND valid_date = ? ORDER BY elevation_band",
                MAPPER, region, Date.valueOf(date));
    }

    public List<AvalancheBulletin> latest() {
        return jdbcTemplate.query("SELECT * FROM bulletin WHERE valid_date = (SELECT MAX(valid_date) FROM bulletin) "
                + "ORDER BY region, elevation_band", MAPPER);
    }

    /** Danger rating keyed by {@code region|BAND} for the newest bulletin date. */
    public Map<String, Integer> latestRatings() {
        Map<String, Integer> ratings = new HashMap<>();
        jdbcTemplate.query("SELECT region, elevation_band, danger_rating FROM bulletin "
                        + "WHERE valid_date = (SELECT MAX(valid_date) FROM bulletin)",
                rs -> {
                    ratings.put(rs.getString("region") + "|" + rs.getString("elevation_band"),
                            rs.getInt("danger_rating"));
                });
        return ratings;
    }

    public int count() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bulletin", Integer.class);
        return count == null ? 0 : count;
    }

    public Optional<LocalDate> latestValidDate() {
        Date date = jdbcTemplate.queryForObject("SELECT MAX(valid_date) FROM bulletin", Date.class);
        return Optional.ofNullable(date).map(Date::toLocalDate);
    }

    public Instant newestIssueTime() {
        Timestamp ts = jdbcTemplate.queryForObject("SELECT MAX(issued_at) FROM bulletin", Timestamp.class);
        return ts == null ? Instant.EPOCH : ts.toInstant();
    }
}
