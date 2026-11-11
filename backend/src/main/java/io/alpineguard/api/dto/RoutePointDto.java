package io.alpineguard.api.dto;

import io.alpineguard.domain.RoutePoint;

public record RoutePointDto(int seq, double lat, double lon, double elevationM, double distanceM,
                            double slopeDeg, double aspectDeg) {

    public static RoutePointDto from(RoutePoint point) {
        return new RoutePointDto(point.seq(), point.lat(), point.lon(), point.elevationM(), point.distanceM(),
                point.slopeDeg(), point.aspectDeg());
    }
}
