package io.alpineguard.ingest;

import io.alpineguard.domain.StationReading;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Reads the hourly station CSV export (SMET style column set). */
@Component
public class SmetReadingParser {

    public List<StationReading> parse(InputStream in) {
        List<StationReading> readings = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                return readings;
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] cells = line.split(",");
                readings.add(new StationReading(
                        cells[0].trim(),
                        Instant.parse(cells[1].trim()),
                        parseDouble(cells[2]),
                        parseDouble(cells[3]),
                        parseDouble(cells[4]),
                        parseDouble(cells[5]),
                        parseDouble(cells[6]),
                        parseDouble(cells[7]),
                        parseDouble(cells[8]),
                        parseDouble(cells[9])));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("unreadable station readings", e);
        }
        return readings;
    }

    private static Double parseDouble(String cell) {
        String trimmed = cell == null ? "" : cell.trim();
        if (trimmed.isEmpty() || "NA".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return Double.valueOf(trimmed);
    }
}
