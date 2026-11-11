package io.alpineguard.domain;

public record RoutePoint(
        int seq,
        double lat,
        double lon,
        double elevationM,
        double distanceM,
        double slopeDeg,
        double aspectDeg) {
}
