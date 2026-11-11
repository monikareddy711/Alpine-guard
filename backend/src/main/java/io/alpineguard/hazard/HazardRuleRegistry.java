package io.alpineguard.hazard;

import io.alpineguard.domain.ElevationBand;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class HazardRuleRegistry {

    private final Map<ElevationBand, HazardRuleStrategy> rules = new EnumMap<>(ElevationBand.class);

    public HazardRuleRegistry(List<HazardRuleStrategy> strategies) {
        for (HazardRuleStrategy strategy : strategies) {
            rules.put(strategy.band(), strategy);
        }
    }

    public HazardRuleStrategy forBand(ElevationBand band) {
        HazardRuleStrategy strategy = rules.get(band);
        if (strategy == null) {
            throw new IllegalStateException("no hazard rule registered for band " + band);
        }
        return strategy;
    }
}
