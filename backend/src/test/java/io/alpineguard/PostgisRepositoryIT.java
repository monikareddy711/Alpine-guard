package io.alpineguard;

import io.alpineguard.domain.Station;
import io.alpineguard.repository.StationRepository;
import io.alpineguard.spatial.SpatialQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the same repository and proximity assertions against PostgreSQL 16 with PostGIS.
 * The container needs a Docker daemon, so the class is skipped on machines without one.
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgisRepositoryIT extends AbstractIntegrationTest {

    /**
     * The official postgis/postgis tag is published for amd64 only; this repository serves the
     * same PostgreSQL 16 / PostGIS 3.4 build as a multi-arch manifest so the test also runs on
     * arm64 machines. Override with POSTGIS_IMAGE when a different image is preferred.
     */
    private static final String POSTGIS_IMAGE =
            System.getenv().getOrDefault("POSTGIS_IMAGE", "imresamu/postgis:16-3.4");

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withDatabaseName("alpineguard")
            .withUsername("alpineguard")
            .withPassword("alpineguard");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGIS::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGIS::getUsername);
        registry.add("spring.datasource.password", POSTGIS::getPassword);
    }

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private SpatialQueryService spatialQueryService;

    @Test
    void postgisSeedAndSpatialQueryAgreeWithTheInMemoryPath() {
        assertThat(stationRepository.count()).isEqualTo(24);
        assertThat(spatialQueryService.byName("postgis").name()).isEqualTo("postgis-dwithin");
        List<String> fromPostgis = spatialQueryService.byName("postgis").within(46.8333, 9.8067, 8000).stream()
                .map(proximity -> proximity.station().stationId()).toList();
        List<String> fromTree = spatialQueryService.byName("kd-tree").within(46.8333, 9.8067, 8000).stream()
                .map(proximity -> proximity.station().stationId()).toList();
        assertThat(fromPostgis).isNotEmpty().containsExactlyElementsOf(fromTree);
    }

    @Test
    void geometryColumnIsPopulatedForEveryStation() {
        List<Station> stations = stationRepository.findAll();
        assertThat(stations).hasSize(24).allSatisfy(station -> {
            assertThat(station.lat()).isNotZero();
            assertThat(station.lon()).isNotZero();
        });
    }
}
