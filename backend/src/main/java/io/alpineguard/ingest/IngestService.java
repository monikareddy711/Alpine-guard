package io.alpineguard.ingest;

import io.alpineguard.api.dto.IngestReportDto;
import io.alpineguard.domain.AvalancheBulletin;
import io.alpineguard.domain.RoutePoint;
import io.alpineguard.domain.SkiRoute;
import io.alpineguard.domain.Station;
import io.alpineguard.domain.StationReading;
import io.alpineguard.repository.BulletinRepository;
import io.alpineguard.repository.ReadingRepository;
import io.alpineguard.repository.RouteRepository;
import io.alpineguard.repository.StationRepository;
import io.alpineguard.spatial.StationIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Normalises the bundled EAWS / SLF shaped feeds into the station, reading, bulletin and
 * route tables. The same parsers read a live feed export when one is dropped into the
 * configured feed directory.
 */
@Service
public class IngestService {

    private static final Logger LOG = LoggerFactory.getLogger(IngestService.class);

    private final LineaStationParser stationParser;
    private final SmetReadingParser readingParser;
    private final CaamlBulletinParser bulletinParser;
    private final GpxRouteParser gpxRouteParser;
    private final StationRepository stationRepository;
    private final ReadingRepository readingRepository;
    private final BulletinRepository bulletinRepository;
    private final RouteRepository routeRepository;
    private final StationIndex stationIndex;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public IngestService(LineaStationParser stationParser, SmetReadingParser readingParser,
                         CaamlBulletinParser bulletinParser, GpxRouteParser gpxRouteParser,
                         StationRepository stationRepository, ReadingRepository readingRepository,
                         BulletinRepository bulletinRepository, RouteRepository routeRepository,
                         StationIndex stationIndex) {
        this.stationParser = stationParser;
        this.readingParser = readingParser;
        this.bulletinParser = bulletinParser;
        this.gpxRouteParser = gpxRouteParser;
        this.stationRepository = stationRepository;
        this.readingRepository = readingRepository;
        this.bulletinRepository = bulletinRepository;
        this.routeRepository = routeRepository;
        this.stationIndex = stationIndex;
    }

    public IngestReportDto ingestBundled() {
        try {
            List<Station> stations = parseStations(resolver.getResource("classpath:seed/stations_linea.json"));
            stationRepository.insertAll(stations);
            stationRepository.backfillGeometry();

            List<StationReading> readings = parseReadings(resolver.getResource("classpath:seed/readings_smet.csv"));
            readingRepository.insertAll(readings);

            List<AvalancheBulletin> bulletins = parseBulletins(
                    resolver.getResource("classpath:seed/bulletins_caaml.json"));
            bulletinRepository.insertAll(bulletins);

            int routes = ingestRoutes();
            stationIndex.refresh();
            LOG.info("ingested {} stations, {} readings, {} bulletins, {} routes",
                    stations.size(), readings.size(), bulletins.size(), routes);
            return new IngestReportDto(stations.size(), readings.size(), bulletins.size(), routes);
        } catch (IOException e) {
            throw new IllegalStateException("bundled seed data is missing", e);
        }
    }

    public List<Station> parseStations(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return stationParser.parse(in);
        }
    }

    public List<StationReading> parseReadings(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return readingParser.parse(in);
        }
    }

    public List<AvalancheBulletin> parseBulletins(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return bulletinParser.parse(in);
        }
    }

    private int ingestRoutes() throws IOException {
        Resource[] gpxFiles = resolver.getResources("classpath:seed/routes/*.gpx");
        List<String> slugs = new ArrayList<>();
        for (Resource gpx : gpxFiles) {
            List<RoutePoint> points;
            try (InputStream in = gpx.getInputStream()) {
                points = gpxRouteParser.buildRoutePoints(gpxRouteParser.readTrackPoints(in));
            }
            RouteMetadata metadata = RouteMetadata.fromFilename(gpx.getFilename());
            SkiRoute route = gpxRouteParser.toRoute(metadata.slug(), metadata.name(), metadata.region(),
                    metadata.difficulty(), "classpath:seed/routes/" + gpx.getFilename(), points);
            routeRepository.insert(route);
            slugs.add(route.slug());
        }
        LOG.debug("ingested routes {}", slugs);
        return gpxFiles.length;
    }

    private record RouteMetadata(String slug, String name, String region, String difficulty) {

        static RouteMetadata fromFilename(String filename) {
            String base = filename.replace(".gpx", "");
            return switch (base) {
                case "stubai-gletscher-schaufelspitze" ->
                        new RouteMetadata(base, "Stubai Glacier to Schaufelspitze", "AT-07 Tyrol", "D4");
                case "weissfluhjoch-parsenn" ->
                        new RouteMetadata(base, "Weissfluhjoch Parsenn descent", "CH-GR Graubuenden", "D2");
                case "sulzkogel-kuehtai" ->
                        new RouteMetadata(base, "Kuehtai to Sulzkogel", "AT-07 Tyrol", "D4");
                case "pitztal-wildspitze" ->
                        new RouteMetadata(base, "Pitztal Glacier to Wildspitze", "AT-07 Tyrol", "D4");
                case "obergurgl-langtalferner" ->
                        new RouteMetadata(base, "Obergurgl Langtalferner", "AT-07 Tyrol", "D3");
                default -> new RouteMetadata(base, base, "AT-07 Tyrol", "D3");
            };
        }
    }
}
