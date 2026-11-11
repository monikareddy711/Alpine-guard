package io.alpineguard.domain;

public enum HazardSeverity {
    LOW,
    MODERATE,
    CONSIDERABLE,
    HIGH,
    EXTREME;

    public static HazardSeverity fromScore(double score) {
        if (score < 25.0) {
            return LOW;
        }
        if (score < 45.0) {
            return MODERATE;
        }
        if (score < 65.0) {
            return CONSIDERABLE;
        }
        if (score < 80.0) {
            return HIGH;
        }
        return EXTREME;
    }

    public int rank() {
        return ordinal() + 1;
    }
}
