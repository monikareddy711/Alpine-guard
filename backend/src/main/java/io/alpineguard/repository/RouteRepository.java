package io.alpineguard.repository;

import io.alpineguard.domain.RoutePoint;
import io.alpineguard.domain.SkiRoute;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RouteRepository {

    private static final RowMapper<SkiRoute> ROUTE_MAPPER = (rs, rowNum) -> new SkiRoute(
            rs.getLong("id"),
            rs.getString("slug"),
            rs.getString("name"),
            rs.getString("region"),
            rs.getString("difficulty"),
            rs.getDouble("distance_m"),
            rs.getDouble("ascent_m"),
            rs.getDouble("descent_m"),
            rs.getDouble("max_elevation_m"),
            rs.getDouble("min_elevation_m"),
            rs.getString("gpx_resource"),
            new ArrayList<>());

    private static final RowMapper<RoutePoint> POINT_MAPPER = (rs, rowNum) -> new RoutePoint(
            rs.getInt("seq"),
            rs.getDouble("lat"),
            rs.getDouble("lon"),
            rs.getDouble("elevation_m"),
            rs.getDouble("distance_m"),
            rs.getDouble("slope_deg"),
            rs.getDouble("aspect_deg"));

    private final JdbcTemplate jdbcTemplate;

    public RouteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SkiRoute> findAll() {
        return jdbcTemplate.query("SELECT * FROM ski_route ORDER BY name", ROUTE_MAPPER);
    }

    public Optional<SkiRoute> findBySlug(String slug) {
        List<SkiRoute> rows = jdbcTemplate.query("SELECT * FROM ski_route WHERE slug = ?", ROUTE_MAPPER, slug);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        SkiRoute route = rows.get(0);
        List<RoutePoint> points = jdbcTemplate.query(
                "SELECT * FROM route_point WHERE route_id = ? ORDER BY seq", POINT_MAPPER, route.id());
        return Optional.of(new SkiRoute(route.id(), route.slug(), route.name(), route.region(), route.difficulty(),
                route.distanceM(), route.ascentM(), route.descentM(), route.maxElevationM(), route.minElevationM(),
                route.gpxResource(), points));
    }

    public int count() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ski_route", Integer.class);
        return count == null ? 0 : count;
    }

    public void insert(SkiRoute route) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO ski_route (slug, name, region, difficulty, distance_m, ascent_m, descent_m, "
                            + "max_elevation_m, min_elevation_m, gpx_resource) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    // Naming the column keeps the driver from returning every column, which
                    // PostgreSQL does for RETURN_GENERATED_KEYS and Spring rejects as multi key.
                    new String[]{"id"});
            ps.setString(1, route.slug());
            ps.setString(2, route.name());
            ps.setString(3, route.region());
            ps.setString(4, route.difficulty());
            ps.setDouble(5, route.distanceM());
            ps.setDouble(6, route.ascentM());
            ps.setDouble(7, route.descentM());
            ps.setDouble(8, route.maxElevationM());
            ps.setDouble(9, route.minElevationM());
            ps.setString(10, route.gpxResource());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        long routeId = key == null ? 0L : key.longValue();
        jdbcTemplate.batchUpdate("INSERT INTO route_point (route_id, seq, lat, lon, elevation_m, distance_m, "
                        + "slope_deg, aspect_deg) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                route.points().stream().map(p -> new Object[]{routeId, p.seq(), p.lat(), p.lon(), p.elevationM(),
                        p.distanceM(), p.slopeDeg(), p.aspectDeg()}).toList());
    }
}
