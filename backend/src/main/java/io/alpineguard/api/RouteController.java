package io.alpineguard.api;

import io.alpineguard.api.dto.HazardAssessmentDto;
import io.alpineguard.api.dto.RouteDetailDto;
import io.alpineguard.api.dto.RouteSummaryDto;
import io.alpineguard.hazard.RouteHazardEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routes")
@Tag(name = "routes", description = "GPX ski tours and their hazard assessments")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    @Operation(summary = "Ski tour summaries with the aggregated hazard score")
    public List<RouteSummaryDto> list(@RequestParam(defaultValue = "72") int windowHours) {
        return routeService.summaries(windowHours);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Route geometry with per point slope and aspect")
    public RouteDetailDto detail(@PathVariable String slug) {
        return routeService.detail(slug);
    }

    @GetMapping("/{slug}/hazard-assessment")
    @Operation(summary = "Segment hazard breakdown for a ski tour")
    public HazardAssessmentDto assessment(@PathVariable String slug,
                                          @RequestParam(defaultValue = "72") int windowHours) {
        if (windowHours < 1 || windowHours > 720) {
            throw new IllegalArgumentException("windowHours must be between 1 and 720");
        }
        return routeService.assessment(slug, windowHours);
    }

    @GetMapping("/{slug}/hazard-assessment/default")
    @Operation(summary = "Segment hazard breakdown using the default window")
    public HazardAssessmentDto defaultAssessment(@PathVariable String slug) {
        return routeService.assessment(slug, RouteHazardEngine.DEFAULT_WINDOW_HOURS);
    }
}
