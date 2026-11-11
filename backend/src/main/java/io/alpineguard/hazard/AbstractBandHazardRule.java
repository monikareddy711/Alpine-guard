package io.alpineguard.hazard;

import io.alpineguard.domain.ElevationBand;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBandHazardRule implements HazardRuleStrategy {

    private static final double SLOPE_RAMP_START_DEG = 25.0;
    private static final double SLOPE_RAMP_END_DEG = 38.0;
    private static final double WIND_REFERENCE_GUST_MS = 25.0;
    private static final double NEW_SNOW_REFERENCE_CM = 40.0;

    private final HazardWeights weights;
    protected final HazardProperties properties;

    protected AbstractBandHazardRule(HazardWeights weights, HazardProperties properties) {
        this.weights = weights;
        this.properties = properties;
    }

    public HazardWeights weights() {
        return weights;
    }

    @Override
    public double evaluate(HazardInput in) {
        double score = 100.0 * (
                weights.slope() * slopeComponent(in.slopeDeg())
                        + weights.wind() * windComponent(in)
                        + weights.snow() * snowComponent(in.newSnow24hCm())
                        + weights.danger() * dangerComponent(in.dangerRating())
                        + weights.temp() * tempComponent(in.airTempC()));
        return Math.max(0.0, Math.min(100.0, score));
    }

    /** Slope alone starts to matter at 25 degrees and saturates near the 38 degree rule of thumb. */
    protected double slopeComponent(double slopeDeg) {
        double ramp = (slopeDeg - SLOPE_RAMP_START_DEG) / (SLOPE_RAMP_END_DEG - SLOPE_RAMP_START_DEG);
        return Math.max(0.0, Math.min(1.0, ramp));
    }

    protected double windComponent(HazardInput in) {
        double gust = Math.max(0.0, in.windGustMs()) / WIND_REFERENCE_GUST_MS;
        return Math.max(0.0, Math.min(1.0, gust * in.leeAlignment()));
    }

    protected double snowComponent(double newSnow24hCm) {
        return Math.max(0.0, Math.min(1.0, newSnow24hCm / NEW_SNOW_REFERENCE_CM));
    }

    protected double dangerComponent(int dangerRating) {
        return Math.max(0.0, Math.min(1.0, (dangerRating - 1) / 4.0));
    }

    protected double tempComponent(double airTempC) {
        return 0.0;
    }

    @Override
    public List<String> flags(HazardInput in) {
        List<String> flags = new ArrayList<>();
        if (in.slopeDeg() >= properties.getSteepSlopeDeg()) {
            flags.add("STEEP_SLOPE");
        }
        if (in.leeAlignment() >= 0.5 && in.windGustMs() >= properties.getWindLoadingGustMs()) {
            flags.add("WIND_LOADED");
        }
        if (in.newSnow24hCm() >= 20.0) {
            flags.add("RECENT_SNOW");
        }
        if (in.stationDistanceM() > 3000.0) {
            flags.add("SPARSE_OBSERVATIONS");
        }
        return flags;
    }

    @Override
    public ElevationBand band() {
        return bandOf();
    }

    protected abstract ElevationBand bandOf();
}
