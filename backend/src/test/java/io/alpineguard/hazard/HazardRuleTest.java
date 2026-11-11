package io.alpineguard.hazard;

import io.alpineguard.domain.ElevationBand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HazardRuleTest {

    private final HazardProperties properties = new HazardProperties();

    private HazardInput input(double slope, double gust, double newSnow, int danger, double lee, double temp) {
        return new HazardInput(slope, 180.0, 2600.0, newSnow, 90.0, gust, 315.0, temp, danger, lee, 1200.0);
    }

    @Test
    void weightsMustSumToOne() {
        assertThatThrownBy(() -> HazardWeights.builder().slope(0.5).wind(0.2).snow(0.1).danger(0.1).temp(0.0).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sum to 1.0");
    }

    @Test
    void lowBandIsBenignOnAGentleSlopeWithoutSnow() {
        LowBandHazardRule rule = new LowBandHazardRule(properties);
        assertThat(rule.evaluate(input(12.0, 2.0, 0.0, 1, 0.0, -6.0))).isLessThan(5.0);
    }

    @Test
    void highBandIsMoreWindSensitiveThanMidBand() {
        HighBandHazardRule high = new HighBandHazardRule(properties);
        MidBandHazardRule mid = new MidBandHazardRule(properties);
        HazardInput windy = input(34.0, 20.0, 10.0, 3, 1.0, -8.0);
        assertThat(high.evaluate(windy)).isGreaterThan(mid.evaluate(windy));
    }

    @Test
    void lowBandAddsWetSnowPenaltyWhenWarm() {
        LowBandHazardRule rule = new LowBandHazardRule(properties);
        HazardInput cold = input(32.0, 5.0, 5.0, 2, 0.2, -4.0);
        HazardInput warm = input(32.0, 5.0, 5.0, 2, 0.2, 6.0);
        assertThat(rule.evaluate(warm)).isGreaterThan(rule.evaluate(cold));
        assertThat(rule.flags(warm)).contains("WET_SNOW");
        assertThat(rule.flags(cold)).doesNotContain("WET_SNOW");
    }

    @Test
    void steepAndWindLoadedFlagsAreRaised() {
        MidBandHazardRule rule = new MidBandHazardRule(properties);
        List<String> flags = rule.flags(input(38.0, 18.0, 25.0, 4, 0.9, -5.0));
        assertThat(flags).contains("STEEP_SLOPE", "WIND_LOADED", "RECENT_SNOW");
    }

    @Test
    void sparseObservationFlagFollowsStationDistance() {
        MidBandHazardRule rule = new MidBandHazardRule(properties);
        HazardInput far = new HazardInput(30.0, 180.0, 2500.0, 5.0, 80.0, 5.0, 315.0, -5.0, 3, 0.4, 4200.0);
        assertThat(rule.flags(far)).contains("SPARSE_OBSERVATIONS");
    }

    @Test
    void scoreIsClampedToHundred() {
        HighBandHazardRule rule = new HighBandHazardRule(properties);
        assertThat(rule.evaluate(input(60.0, 45.0, 90.0, 5, 1.0, 10.0))).isLessThanOrEqualTo(100.0);
        assertThat(rule.evaluate(input(0.0, 0.0, 0.0, 1, 0.0, -20.0))).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void registryResolvesEveryBand() {
        HazardRuleRegistry registry = new HazardRuleRegistry(List.of(
                new LowBandHazardRule(properties), new MidBandHazardRule(properties), new HighBandHazardRule(properties)));
        assertThat(registry.forBand(ElevationBand.LOW)).isInstanceOf(LowBandHazardRule.class);
        assertThat(registry.forBand(ElevationBand.MID)).isInstanceOf(MidBandHazardRule.class);
        assertThat(registry.forBand(ElevationBand.HIGH)).isInstanceOf(HighBandHazardRule.class);
    }

    @Test
    void registryRejectsIncompleteRuleSets() {
        HazardRuleRegistry registry = new HazardRuleRegistry(List.of(new LowBandHazardRule(properties)));
        assertThatThrownBy(() -> registry.forBand(ElevationBand.HIGH))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bandSelectionFollowsElevation() {
        assertThat(ElevationBand.forElevation(1500)).isEqualTo(ElevationBand.LOW);
        assertThat(ElevationBand.forElevation(2500)).isEqualTo(ElevationBand.MID);
        assertThat(ElevationBand.forElevation(3000)).isEqualTo(ElevationBand.HIGH);
    }
}
