package io.alpineguard.domain;

import java.time.Instant;
import java.time.LocalDate;

public record AvalancheBulletin(
        String region,
        LocalDate validDate,
        ElevationBand elevationBand,
        int dangerRating,
        String mainProblem,
        String summary,
        Instant issuedAt) {
}
