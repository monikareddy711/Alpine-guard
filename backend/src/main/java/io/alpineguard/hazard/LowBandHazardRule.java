package io.alpineguard.hazard;

import io.alpineguard.domain.ElevationBand;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Below 2000 m the snowpack is shallower and wet snow activity driven by positive air
 * temperature adds a term that does not exist in the higher bands.
 */
@Component
public class LowBandHazardRule extends AbstractBandHazardRule {

    public LowBandHazardRule(HazardProperties properties) {
        super(HazardWeights.builder()
                .slope(0.28).wind(0.18).snow(0.22).danger(0.22).temp(0.10)
                .build(), properties);
    }

    @Override
    protected double tempComponent(double airTempC) {
        return Math.max(0.0, Math.min(1.0, (airTempC - 2.0) / 8.0));
    }

    @Override
    public List<String> flags(HazardInput in) {
        List<String> flags = super.flags(in);
        if (in.airTempC() > 2.0 && in.slopeDeg() >= properties.getSteepSlopeDeg()) {
            flags.add("WET_SNOW");
        }
        return flags;
    }

    @Override
    protected ElevationBand bandOf() {
        return ElevationBand.LOW;
    }
}
