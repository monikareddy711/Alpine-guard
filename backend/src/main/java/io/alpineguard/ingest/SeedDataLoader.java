package io.alpineguard.ingest;

import io.alpineguard.repository.RouteRepository;
import io.alpineguard.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedDataLoader implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SeedDataLoader.class);

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final IngestService ingestService;
    private final boolean seedOnStartup;

    public SeedDataLoader(StationRepository stationRepository, RouteRepository routeRepository,
                          IngestService ingestService,
                          @Value("${alpineguard.ingest.seed-on-startup:true}") boolean seedOnStartup) {
        this.stationRepository = stationRepository;
        this.routeRepository = routeRepository;
        this.ingestService = ingestService;
        this.seedOnStartup = seedOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seedOnStartup) {
            LOG.info("seed on startup disabled");
            return;
        }
        int stations = stationRepository.count();
        int routes = routeRepository.count();
        if (stations > 0) {
            if (routes == 0) {
                // A previous run stopped between the station and route inserts, so the tables
                // are inconsistent. Loading again would violate the station uniqueness keys,
                // therefore the operator has to reset the database.
                LOG.warn("station table holds {} rows but no routes; reset the database "
                        + "(docker compose down -v) to load the bundled feeds again", stations);
            } else {
                LOG.info("station table already populated with {} stations and {} routes, skipping seed",
                        stations, routes);
            }
            return;
        }
        ingestService.ingestBundled();
    }
}
