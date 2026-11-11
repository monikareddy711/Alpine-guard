package io.alpineguard.hazard;

import io.alpineguard.domain.ElevationBand;
import org.springframework.stereotype.Component;

@Component
public class MidBandHazardRule extends AbstractBandHazardRule {

    public MidBandHazardRule(HazardProperties properties) {
        super(HazardWeights.builder()
                .slope(0.34).wind(0.28).snow(0.20).danger(0.18).temp(0.0)
                .build(), properties);
    }

    @Override
    protected ElevationBand bandOf() {
        return ElevationBand.MID;
    }
}
