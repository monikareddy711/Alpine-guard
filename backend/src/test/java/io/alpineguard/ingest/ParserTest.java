package io.alpineguard.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.alpineguard.domain.AvalancheBulletin;
import io.alpineguard.domain.ElevationBand;
import io.alpineguard.domain.RoutePoint;
import io.alpineguard.domain.SkiRoute;
import io.alpineguard.domain.Station;
import io.alpineguard.domain.StationReading;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ParserTest {

    private static ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void stationFeedIsReadFromGeoJsonFeatures() {
        String json = """
                {"type":"FeatureCollection","features":[
                  {"type":"Feature","geometry":{"type":"Point","coordinates":[9.8067,46.8333]},
                   "properties":{"stationId":"CHGR-201","name":"Weissfluhjoch","region":"CH-GR Graubuenden",
                   "operator":"SLF","network":"IMIS","elevationM":2693,"active":true}}]}
                """;
        List<Station> stations = new LineaStationParser(new ObjectMapper()).parse(stream(json));
        assertThat(stations).hasSize(1);
        Station station = stations.get(0);
        assertThat(station.stationId()).isEqualTo("CHGR-201");
        assertThat(station.lat()).isEqualTo(46.8333);
        assertThat(station.lon()).isEqualTo(9.8067);
        assertThat(station.band()).isEqualTo(ElevationBand.MID);
    }

    @Test
    void readingsCsvKeepsNullsForMissingCells() {
        String csv = """
                station_id,measured_at,air_temp_c,wind_speed_ms,wind_gust_ms,wind_dir_deg,snow_depth_cm,new_snow_cm,humidity_pct,pressure_hpa
                AT07-103,2026-03-15T10:00:00Z,-3.5,4.2,9.1,310,142.0,1.5,78,845.0
                AT07-103,2026-03-15T11:00:00Z,,4.4,NA,312,143.0,0.0,80,844.0
                """;
        List<StationReading> readings = new SmetReadingParser().parse(stream(csv));
        assertThat(readings).hasSize(2);
        assertThat(readings.get(0).airTempC()).isEqualTo(-3.5);
        assertThat(readings.get(1).airTempC()).isNull();
        assertThat(readings.get(1).windGustMs()).isNull();
    }

    @Test
    void bulletinsExpandPerElevationBand() {
        String json = """
                {"bulletins":[{"region":"AT-07 Tyrol","validDate":"2026-03-16","issuedAt":"2026-03-16T07:00:00Z",
                "mainProblem":"WIND_SLAB","summary":"Fresh wind slabs.",
                "dangerRatings":[{"band":"LOW","rating":2},{"band":"HIGH","rating":4}]}]}
                """;
        List<AvalancheBulletin> bulletins = new CaamlBulletinParser(new ObjectMapper()).parse(stream(json));
        assertThat(bulletins).hasSize(2);
        assertThat(bulletins).extracting(AvalancheBulletin::elevationBand)
                .containsExactly(ElevationBand.LOW, ElevationBand.HIGH);
        assertThat(bulletins.get(1).dangerRating()).isEqualTo(4);
    }

    @Test
    void gpxTrackBecomesRouteWithSlopeAndAspect() {
        String gpx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1"><trk><trkseg>
                  <trkpt lat="47.1900" lon="11.0300"><ele>2020.0</ele></trkpt>
                  <trkpt lat="47.1905" lon="11.0300"><ele>2050.0</ele></trkpt>
                  <trkpt lat="47.1910" lon="11.0300"><ele>2080.0</ele></trkpt>
                </trkseg></trk></gpx>
                """;
        GpxRouteParser parser = new GpxRouteParser();
        List<GpxRouteParser.RawPoint> raw = parser.readTrackPoints(stream(gpx));
        assertThat(raw).hasSize(3);
        List<RoutePoint> points = parser.buildRoutePoints(raw);
        assertThat(points).hasSize(3);
        assertThat(points.get(1).distanceM()).isGreaterThan(50.0);
        assertThat(points.get(0).slopeDeg()).isGreaterThan(25.0);
        // Travelling north, the fall line points south.
        assertThat(points.get(0).aspectDeg()).isCloseTo(180.0, within(1.0));
    }

    @Test
    void routeSummaryAccumulatesAscentAndDescent() {
        String gpx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <gpx version="1.1" xmlns="http://www.topografix.com/GPX/1/1"><trk><trkseg>
                  <trkpt lat="46.97" lon="11.08"><ele>2500.0</ele></trkpt>
                  <trkpt lat="46.971" lon="11.08"><ele>2600.0</ele></trkpt>
                  <trkpt lat="46.972" lon="11.08"><ele>2550.0</ele></trkpt>
                </trkseg></trk></gpx>
                """;
        GpxRouteParser parser = new GpxRouteParser();
        List<RoutePoint> points = parser.buildRoutePoints(parser.readTrackPoints(stream(gpx)));
        SkiRoute route = parser.toRoute("demo", "Demo", "AT-07 Tyrol", "D2", "classpath:demo.gpx", points);
        assertThat(route.ascentM()).isEqualTo(100.0);
        assertThat(route.descentM()).isEqualTo(50.0);
        assertThat(route.maxElevationM()).isEqualTo(2600.0);
        assertThat(route.minElevationM()).isEqualTo(2500.0);
    }

    @Test
    void brokenGpxIsRejected() {
        GpxRouteParser parser = new GpxRouteParser();
        assertThatThrownBy(() -> parser.readTrackPoints(stream("<gpx><trk>")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unreadable GPX");
    }
}
