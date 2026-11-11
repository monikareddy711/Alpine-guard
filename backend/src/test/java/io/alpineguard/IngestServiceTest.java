package io.alpineguard;

import io.alpineguard.ingest.IngestService;
import io.alpineguard.repository.BulletinRepository;
import io.alpineguard.repository.ReadingRepository;
import io.alpineguard.repository.RouteRepository;
import io.alpineguard.repository.StationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class IngestServiceTest extends AbstractIntegrationTest {

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private ReadingRepository readingRepository;

    @Autowired
    private BulletinRepository bulletinRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private IngestService ingestService;

    @Test
    void seedLoadsStationsReadingsBulletinsAndRoutes() {
        assertThat(stationRepository.count()).isEqualTo(24);
        assertThat(readingRepository.count()).isGreaterThan(15_000);
        assertThat(bulletinRepository.count()).isGreaterThan(20);
        assertThat(routeRepository.count()).isEqualTo(5);
        assertThat(readingRepository.latestTimestamp()).isPresent();
        assertThat(bulletinRepository.latestRatings()).containsKey("AT-07 Tyrol|HIGH");
    }

    @Test
    void stationsCarrySpatialGeometry() throws Exception {
        assertThat(ingestService.parseStations(new ClassPathResource("seed/stations_linea.json")))
                .hasSize(24)
                .allSatisfy(station -> {
                    assertThat(station.elevationM()).isGreaterThan(300.0);
                    assertThat(station.lat()).isBetween(46.0, 47.5);
                    assertThat(station.lon()).isBetween(9.0, 12.0);
                });
    }

    @Test
    void readingAggregatesCoverEveryStation() {
        var reference = readingRepository.latestTimestamp().orElseThrow();
        var aggregates = readingRepository.aggregate(reference.minusSeconds(72 * 3600),
                reference.minusSeconds(24 * 3600));
        assertThat(aggregates).hasSize(24);
        assertThat(aggregates.values()).allSatisfy(aggregate -> {
            assertThat(aggregate.latest()).isNotNull();
            assertThat(aggregate.newSnow24hCm()).isGreaterThanOrEqualTo(0.0);
        });
    }
}
