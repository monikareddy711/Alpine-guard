package io.alpineguard.spatial;

import io.alpineguard.domain.GeoMath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class KdTreeTest {

    private final double[] lats = {46.83, 46.87, 47.19, 46.92, 47.42, 46.47, 46.98};
    private final double[] lons = {9.81, 11.03, 11.03, 10.88, 10.99, 9.73, 11.10};

    @Test
    void nearestMatchesBruteForce() {
        KdTree tree = new KdTree(lats, lons);
        assertThat(tree.nearest(46.90, 10.90)).isEqualTo(3);
    }

    @Test
    void nearestAgreesWithLinearScanOnRandomQueries() {
        KdTree tree = new KdTree(lats, lons);
        Random random = new Random(11L);
        int checked = 0;
        for (int i = 0; i < 200; i++) {
            double lat = 46.2 + random.nextDouble() * 1.4;
            double lon = 9.4 + random.nextDouble() * 2.2;
            int expected = 0;
            double best = Double.MAX_VALUE;
            double second = Double.MAX_VALUE;
            for (int j = 0; j < lats.length; j++) {
                double d = GeoMath.haversineM(lat, lon, lats[j], lons[j]);
                if (d < best) {
                    second = best;
                    best = d;
                    expected = j;
                } else if (d < second) {
                    second = d;
                }
            }
            // The tree works on a local metric projection, so it is only guaranteed to agree
            // with the great circle scan while the nearest station is not a near tie.
            if ((second - best) / best > 0.02) {
                checked++;
                assertThat(tree.nearest(lat, lon)).isEqualTo(expected);
            }
        }
        assertThat(checked).isGreaterThan(150);
    }

    @Test
    void withinRadiusReturnsOnlyNearbyStations() {
        KdTree tree = new KdTree(lats, lons);
        List<Integer> hits = tree.within(47.19, 11.03, 5000);
        assertThat(hits).contains(2);
        assertThat(hits).doesNotContain(0);
    }

    @Test
    void emptyTreeHasNoHits() {
        KdTree tree = new KdTree(new double[0], new double[0]);
        assertThat(tree.size()).isZero();
        assertThat(tree.nearest(46.8, 9.8)).isEqualTo(-1);
        assertThat(tree.within(46.8, 9.8, 1000)).isEmpty();
    }
}
