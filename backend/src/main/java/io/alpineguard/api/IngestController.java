package io.alpineguard.api;

import io.alpineguard.api.dto.IngestReportDto;
import io.alpineguard.ingest.IngestService;
import io.alpineguard.repository.StationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingest")
@Tag(name = "ingest", description = "Reload the bundled station, reading and bulletin feeds")
public class IngestController {

    private final IngestService ingestService;
    private final StationRepository stationRepository;

    public IngestController(IngestService ingestService, StationRepository stationRepository) {
        this.ingestService = ingestService;
        this.stationRepository = stationRepository;
    }

    @PostMapping("/refresh")
    @Operation(summary = "Re-run the feed ingestion, only when the station table is empty unless force is set")
    public IngestReportDto refresh(@RequestParam(defaultValue = "false") boolean force) {
        if (!force && stationRepository.count() > 0) {
            throw new IllegalArgumentException("station table is populated; pass force=true to append the bundled feeds");
        }
        return ingestService.ingestBundled();
    }
}
