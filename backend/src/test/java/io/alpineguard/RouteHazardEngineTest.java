package io.alpineguard;

import io.alpineguard.domain.HazardSeverity;
import io.alpineguard.domain.RouteHazardAssessment;
import io.alpineguard.domain.SegmentHazard;
import io.alpineguard.domain.SkiRoute;
import io.alpineguard.hazard.RouteHazardEngine;
import io.alpineguard.hazard.RouteNotFoundException;
import io.alpineguard.repository.RouteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteHazardEngineTest extends AbstractIntegrationTest {

    @Autowired
    private RouteHazardEngine engine;

    @Autowired
    private RouteRepository routeRepository;

    @Test
    void assessmentCoversEverySegmentOfTheRoute() {
        RouteHazardAssessment assessment = engine.assess("stubai-gletscher-schaufelspitze", 72);
        SkiRoute route = routeRepository.findBySlug("stubai-gletscher-schaufelspitze").orElseThrow();
        assertThat(assessment.totalSegments()).isEqualTo(route.points().size() - 1);
        assertThat(assessment.segments()).hasSize(route.points().size() - 1);
        assertThat(assessment.windowHours()).isEqualTo(72);
        assertThat(assessment.maxScore()).isBetween(0.0, 100.0);
        assertThat(assessment.maxSlopeDeg()).isGreaterThan(0.0);
    }

    @Test
    void everySegmentCarriesAStationAndASeverity() {
        RouteHazardAssessment assessment = engine.assess("pitztal-wildspitze", 72);
        assertThat(assessment.segments()).allSatisfy(segment -> {
            assertThat(segment.nearestStationId()).isNotBlank();
            assertThat(segment.severity()).isNotNull();
            assertThat(segment.aspectDeg()).isBetween(0.0, 360.0);
        });
    }

    @Test
    void steepSegmentsAreCounted() {
        RouteHazardAssessment assessment = engine.assess("pitztal-wildspitze", 72);
        long steep = assessment.segments().stream().filter(segment -> segment.slopeDeg() >= 30.0).count();
        assertThat(assessment.steepSegmentCount()).isEqualTo((int) steep);
    }

    @Test
    void repeatedAssessmentsAreIdentical() {
        RouteHazardAssessment first = engine.assess("weissfluhjoch-parsenn", 72);
        RouteHazardAssessment second = engine.assess("weissfluhjoch-parsenn", 72);
        assertThat(second.segments()).containsExactlyElementsOf(first.segments());
        assertThat(second.maxScore()).isEqualTo(first.maxScore());
    }

    @Test
    void unknownRouteRaisesNotFound() {
        assertThatThrownBy(() -> engine.assess("nope", 72)).isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void shortWindowStillProducesAnAssessment() {
        RouteHazardAssessment assessment = engine.assess("sulzkogel-kuehtai", 6);
        assertThat(assessment.windowHours()).isEqualTo(6);
        assertThat(assessment.segments()).isNotEmpty();
    }

    @Test
    void segmentSeverityMatchesItsScore() {
        RouteHazardAssessment assessment = engine.assess("obergurgl-langtalferner", 72);
        for (SegmentHazard segment : assessment.segments()) {
            assertThat(segment.severity()).isEqualTo(HazardSeverity.fromScore(segment.score()));
        }
    }
}
