package io.alpineguard;

import io.alpineguard.domain.Station;
import io.alpineguard.spatial.SpatialQueryService;
import io.alpineguard.spatial.SpatialQueryStrategy;
import io.alpineguard.spatial.StationIndex;
import io.alpineguard.spatial.StationProximity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpatialStrategyParityTest extends AbstractIntegrationTest {

    @Autowired
    private SpatialQueryService spatialQueryService;

    @Autowired
    private StationIndex stationIndex;

    @Test
    void bothStrategiesExposeThemselves() {
        assertThat(spatialQueryService.all()).containsKeys("kd-tree");
        assertThat(spatialQueryService.all().keySet()).anyMatch(name -> name.endsWith("-dwithin"));
        assertThat(spatialQueryService.defaultStrategyName()).isEqualTo("kd-tree");
    }

    @Test
    void databaseAndTreeStrategiesReturnTheSameNeighbours() {
        stationIndex.refresh();
        SpatialQueryStrategy database = spatialQueryService.byName("h2gis");
        SpatialQueryStrategy tree = spatialQueryService.byName("kd-tree");
        double[][] probes = {
                {46.8333, 9.8067}, {47.1900, 11.0300}, {46.9167, 10.8833},
                {46.4700, 9.7300}, {47.4000, 10.9800}, {46.7000, 10.5000}
        };
        for (double[] probe : probes) {
            List<String> fromDatabase = database.within(probe[0], probe[1], 8000).stream()
                    .map(proximity -> proximity.station().stationId()).toList();
            List<String> fromTree = tree.within(probe[0], probe[1], 8000).stream()
                    .map(proximity -> proximity.station().stationId()).toList();
            assertThat(fromTree).containsExactlyElementsOf(fromDatabase);
        }
    }

    @Test
    void nearestStationRespectsTheRadius() {
        StationProximity proximity = spatialQueryService.nearest(46.8333, 9.8067, 5000).orElseThrow();
        assertThat(proximity.distanceM()).isLessThan(200.0);
        assertThat(proximity.station().region()).isEqualTo("CH-GR Graubuenden");
        assertThat(spatialQueryService.nearest(46.70, 10.40, 10)).isEmpty();
    }

    @Test
    void unknownStrategyFallsBackToTheDefault() {
        SpatialQueryStrategy fallback = spatialQueryService.byName("does-not-exist");
        assertThat(fallback.name()).isNotBlank();
        assertThat(spatialQueryService.active().within(46.83, 9.80, 5000)).isNotEmpty();
    }

    @Test
    void stationIndexHoldsTheSeededNetwork() {
        StationIndex.Snapshot snapshot = stationIndex.snapshot();
        assertThat(snapshot.stations()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(snapshot.tree().size()).isEqualTo(snapshot.stations().size());
        assertThat(snapshot.stations()).extracting(Station::region)
                .contains("AT-07 Tyrol", "CH-GR Graubuenden");
    }
}
