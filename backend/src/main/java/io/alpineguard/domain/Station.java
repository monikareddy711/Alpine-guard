package io.alpineguard.domain;

public record Station(
        Long id,
        String stationId,
        String name,
        String region,
        String operator,
        String network,
        double elevationM,
        double lat,
        double lon,
        boolean active) {

    public ElevationBand band() {
        return ElevationBand.forElevation(elevationM);
    }
}
