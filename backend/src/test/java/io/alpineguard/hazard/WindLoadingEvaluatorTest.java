package io.alpineguard.hazard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WindLoadingEvaluatorTest {

    private final WindLoadingEvaluator evaluator = new WindLoadingEvaluator(new HazardProperties());

    @Test
    void leeSlopeFacingAwayFromTheWindIsFullyLoaded() {
        // Wind from 315 degrees loads south east facing slopes, aspect 135 degrees.
        assertThat(evaluator.leeAlignment(135.0, 315.0)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void windwardSlopeIsNotLoaded() {
        assertThat(evaluator.leeAlignment(315.0, 315.0)).isZero();
    }

    @Test
    void alignmentFallsOffLinearly() {
        assertThat(evaluator.leeAlignment(112.5, 315.0)).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void beyondToleranceThereIsNoLoading() {
        assertThat(evaluator.leeAlignment(60.0, 315.0)).isZero();
    }
}
