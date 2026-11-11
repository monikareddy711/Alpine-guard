package io.alpineguard.domain;

/**
 * Alpine elevation bands used to select the hazard rule set. The 2000 m and 2800 m
 * boundaries follow the band split used by the Tyrol and Graubünden bulletins, where
 * the treeline band and the high alpine band carry different snow transport behaviour.
 */
public enum ElevationBand {
    LOW(0, 2000),
    MID(2000, 2800),
    HIGH(2800, Double.MAX_VALUE);

    private final double lower;
    private final double upper;

    ElevationBand(double lower, double upper) {
        this.lower = lower;
        this.upper = upper;
    }

    public double lower() {
        return lower;
    }

    public double upper() {
        return upper;
    }

    public static ElevationBand forElevation(double elevationM) {
        if (elevationM < MID.lower) {
            return LOW;
        }
        if (elevationM < HIGH.lower) {
            return MID;
        }
        return HIGH;
    }
}
