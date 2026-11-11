package io.alpineguard.spatial;

import io.alpineguard.domain.Station;

public record StationProximity(Station station, double distanceM) {
}
