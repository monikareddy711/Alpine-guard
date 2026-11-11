package io.alpineguard.hazard;

public record HazardInput(
        double slopeDeg,
        double aspectDeg,
        double elevationM,
        double newSnow24hCm,
        double snowDepthCm,
        double windGustMs,
        double windDirDeg,
        double airTempC,
        int dangerRating,
        double leeAlignment,
        double stationDistanceM) {
}
