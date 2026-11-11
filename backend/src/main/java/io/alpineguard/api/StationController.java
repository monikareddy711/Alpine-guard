package io.alpineguard.api;

import io.alpineguard.api.dto.StationDto;
import io.alpineguard.api.dto.TelemetryDto;
import io.alpineguard.domain.ElevationBand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stations")
@Tag(name = "stations", description = "Weather station metadata, telemetry and map features")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    @Operation(summary = "List active weather stations with the newest observation")
    public List<StationDto> list(@RequestParam(required = false) String region,
                                 @RequestParam(required = false) Double minElevation,
                                 @RequestParam(required = false) Double maxElevation,
                                 @RequestParam(required = false) ElevationBand band,
                                 @RequestParam(required = false) Integer minDanger) {
        return stationService.list(region, minElevation, maxElevation, band, minDanger);
    }

    @GetMapping("/geojson")
    @Operation(summary = "Weather stations as a GeoJSON feature collection")
    public Map<String, Object> geojson() {
        return stationService.featureCollection();
    }

    @GetMapping("/{stationId}")
    @Operation(summary = "Single station")
    public ResponseEntity<StationDto> byId(@PathVariable String stationId) {
        return stationService.byId(stationId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{stationId}/telemetry")
    @Operation(summary = "Hourly telemetry series, 72 hours by default")
    public TelemetryDto telemetry(@PathVariable String stationId,
                                  @RequestParam(defaultValue = "72") int hours) {
        if (hours < 1 || hours > 720) {
            throw new IllegalArgumentException("hours must be between 1 and 720");
        }
        return stationService.telemetry(stationId, hours);
    }
}
