package io.alpineguard.domain;

/** Spherical geometry helpers shared by the hazard engine and the proximity strategies. */
public final class GeoMath {

    public static final double EARTH_RADIUS_M = 6371008.8;

    private GeoMath() {
    }

    public static double haversineM(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** Initial bearing from point 1 to point 2 in degrees clockwise from true north. */
    public static double bearingDeg(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dl = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return normalizeDegrees(Math.toDegrees(Math.atan2(y, x)));
    }

    public static double normalizeDegrees(double deg) {
        double wrapped = deg % 360.0;
        return wrapped < 0 ? wrapped + 360.0 : wrapped;
    }

    /** Smallest absolute difference between two compass directions, in degrees. */
    public static double angularDifferenceDeg(double a, double b) {
        double d = Math.abs(normalizeDegrees(a) - normalizeDegrees(b));
        return d > 180.0 ? 360.0 - d : d;
    }

    public static double slopeDegrees(double elevationDeltaM, double horizontalDistanceM) {
        if (horizontalDistanceM <= 0.001) {
            return 0.0;
        }
        return Math.toDegrees(Math.atan2(elevationDeltaM, horizontalDistanceM));
    }

    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
