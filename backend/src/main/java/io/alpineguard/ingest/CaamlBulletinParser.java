package io.alpineguard.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.alpineguard.domain.AvalancheBulletin;
import io.alpineguard.domain.ElevationBand;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a CAAML v6 shaped bulletin export. Only the fields the dashboard needs are read:
 * the region, the validity date and the danger rating per elevation band.
 */
@Component
public class CaamlBulletinParser {

    private final ObjectMapper objectMapper;

    public CaamlBulletinParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<AvalancheBulletin> parse(InputStream in) {
        try {
            JsonNode root = objectMapper.readTree(in);
            JsonNode bulletins = root.has("bulletins") ? root.get("bulletins") : root;
            List<AvalancheBulletin> parsed = new ArrayList<>();
            for (JsonNode bulletin : bulletins) {
                String region = bulletin.path("region").asText();
                LocalDate validDate = LocalDate.parse(bulletin.path("validDate").asText());
                Instant issuedAt = Instant.parse(bulletin.path("issuedAt").asText());
                String mainProblem = bulletin.path("mainProblem").asText(null);
                String summary = bulletin.path("summary").asText(null);
                for (JsonNode rating : bulletin.path("dangerRatings")) {
                    parsed.add(new AvalancheBulletin(region, validDate,
                            ElevationBand.valueOf(rating.path("band").asText()),
                            rating.path("rating").asInt(),
                            mainProblem, summary, issuedAt));
                }
            }
            return parsed;
        } catch (IOException e) {
            throw new IllegalArgumentException("unreadable bulletin feed", e);
        }
    }
}
