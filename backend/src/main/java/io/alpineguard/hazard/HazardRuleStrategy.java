package io.alpineguard.hazard;

import io.alpineguard.domain.ElevationBand;

import java.util.List;

/** Swappable scoring rule set, one per alpine elevation band. */
public interface HazardRuleStrategy {

    ElevationBand band();

    double evaluate(HazardInput input);

    List<String> flags(HazardInput input);
}
