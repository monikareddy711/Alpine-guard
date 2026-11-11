package io.alpineguard.repository;

import io.alpineguard.domain.StationReading;

public record ReadingAggregate(StationReading latest, double newSnow24hCm) {
}
