package io.alpineguard.hazard;

import io.alpineguard.domain.GeoMath;
import org.springframework.stereotype.Component;

/**
 * Wind loading grows on lee slopes: the aspect that faces away from the wind direction
 * receives the transported snow. Alignment falls off linearly to zero at the tolerance
 * angle so a slope 30 degrees off the lee axis still counts, partially.
 */
@Component
public class WindLoadingEvaluator {

    private final HazardProperties properties;

    public WindLoadingEvaluator(HazardProperties properties) {
        this.properties = properties;
    }

    public double leeAlignment(double aspectDeg, double windDirFromDeg) {
        double leeDirection = GeoMath.normalizeDegrees(windDirFromDeg + 180.0);
        double difference = GeoMath.angularDifferenceDeg(aspectDeg, leeDirection);
        double tolerance = properties.getLeeToleranceDeg();
        if (difference >= tolerance) {
            return 0.0;
        }
        return 1.0 - difference / tolerance;
    }
}
