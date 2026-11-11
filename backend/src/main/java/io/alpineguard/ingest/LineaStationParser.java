package io.alpineguard.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.alpineguard.domain.Station;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the GeoJSON station list published by the EAWS / Albina open data endpoint
 * (weather station layer, one feature per station).
 */
@Component
public class LineaStationParser {

    private final ObjectMapper objectMapper;

    public LineaStationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Station> parse(InputStream in) {
        try {
            JsonNode root = objectMapper.readTree(in);
            JsonNode features = root.has("features") ? root.get("features") : root;
            List<Station> stations = new ArrayList<>();
            for (JsonNode feature : features) {
                JsonNode properties = feature.has("properties") ? feature.get("properties") : feature;
                JsonNode coordinates = feature.path("geometry").path("coordinates");
                double lon = coordinates.isArray() && coordinates.size() > 0
                        ? coordinates.get(0).asDouble() : properties.path("lon").asDouble();
                double lat = coordinates.isArray() && coordinates.size() > 1
                        ? coordinates.get(1).asDouble() : properties.path("lat").asDouble();
                stations.add(new Station(null,
                        properties.path("stationId").asText(),
                        properties.path("name").asText(),
                        properties.path("region").asText(),
                        properties.path("operator").asText(),
                        properties.path("network").asText("LINEA"),
                        properties.path("elevationM").asDouble(),
                        lat, lon,
                        properties.path("active").asBoolean(true)));
            }
            return stations;
        } catch (IOException e) {
            throw new IllegalArgumentException("unreadable station feed", e);
        }
    }
}
