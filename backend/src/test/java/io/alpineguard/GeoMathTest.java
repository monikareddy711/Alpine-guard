package io.alpineguard;

import io.alpineguard.domain.GeoMath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoMathTest {

    @Test
    @DisplayName("haversine distance between Weissfluhjoch and Davos Dorf is about 4.2 km")
    void haversineDistance() {
        double distance = GeoMath.haversineM(46.8333, 9.8067, 46.8, 9.84);
        assertThat(distance).isBetween(3900.0, 4600.0);
    }

    @Test
    void haversineIsZeroForSamePoint() {
        assertThat(GeoMath.haversineM(46.97, 11.10, 46.97, 11.10)).isZero();
    }

    @Test
    void bearingDueNorthAndEast() {
        assertThat(GeoMath.bearingDeg(46.0, 11.0, 47.0, 11.0)).isCloseTo(0.0, within(0.1));
        assertThat(GeoMath.bearingDeg(46.0, 11.0, 46.0, 12.0)).isCloseTo(89.6, within(0.3));
    }

    @Test
    void normalizeWrapsNegativeAngles() {
        assertThat(GeoMath.normalizeDegrees(-90.0)).isCloseTo(270.0, within(1e-9));
        assertThat(GeoMath.normalizeDegrees(450.0)).isCloseTo(90.0, within(1e-9));
    }

    @Test
    void angularDifferenceTakesShortestArc() {
        assertThat(GeoMath.angularDifferenceDeg(350.0, 10.0)).isCloseTo(20.0, within(1e-9));
        assertThat(GeoMath.angularDifferenceDeg(10.0, 350.0)).isCloseTo(20.0, within(1e-9));
    }

    @Test
    void slopeAngleFromRiseAndRun() {
        assertThat(GeoMath.slopeDegrees(100.0, 100.0)).isCloseTo(45.0, within(1e-6));
        assertThat(GeoMath.slopeDegrees(0.0, 100.0)).isZero();
        assertThat(GeoMath.slopeDegrees(-100.0, 173.2)).isCloseTo(-30.0, within(0.01));
    }

    @Test
    void slopeOfZeroDistanceIsZero() {
        assertThat(GeoMath.slopeDegrees(50.0, 0.0)).isZero();
    }

    @Test
    void roundingUsesDecimalPlaces() {
        assertThat(GeoMath.round(12.3456, 2)).isEqualTo(12.35);
        assertThat(GeoMath.round(12.3456, 0)).isEqualTo(12.0);
    }
}
