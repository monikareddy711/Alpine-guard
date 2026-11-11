package io.alpineguard.hazard;

import io.alpineguard.domain.ElevationBand;
import org.springframework.stereotype.Component;

@Component
public class HighBandHazardRule extends AbstractBandHazardRule {

    public HighBandHazardRule(HazardProperties properties) {
        super(HazardWeights.builder()
                .slope(0.30).wind(0.40).snow(0.16).danger(0.14).temp(0.0)
                .build(), properties);
    }

    @Override
    protected ElevationBand bandOf() {
        return ElevationBand.HIGH;
    }
}
