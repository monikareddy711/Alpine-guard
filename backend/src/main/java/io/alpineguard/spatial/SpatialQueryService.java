package io.alpineguard.spatial;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Facade that exposes the configured default strategy and the alternatives by name. */
@Service
public class SpatialQueryService {

    private final Map<String, SpatialQueryStrategy> strategies;
    private final String defaultStrategy;

    public SpatialQueryService(List<SpatialQueryStrategy> available,
                               @Value("${alpineguard.spatial.default-strategy:kd-tree}") String defaultStrategy) {
        this.strategies = available.stream()
                .collect(Collectors.toMap(SpatialQueryStrategy::name, strategy -> strategy));
        this.defaultStrategy = resolveName(defaultStrategy);
    }

    private String resolveName(String requested) {
        if (strategies.containsKey(requested)) {
            return requested;
        }
        if ("postgis".equals(requested) && strategies.containsKey("postgis-dwithin")) {
            return "postgis-dwithin";
        }
        if ("h2gis".equals(requested) && strategies.containsKey("h2gis-dwithin")) {
            return "h2gis-dwithin";
        }
        return strategies.keySet().stream().sorted().findFirst().orElse("kd-tree");
    }

    public String defaultStrategyName() {
        return defaultStrategy;
    }

    public SpatialQueryStrategy active() {
        return strategies.get(defaultStrategy);
    }

    public SpatialQueryStrategy byName(String name) {
        return strategies.get(resolveName(name));
    }

    public Map<String, SpatialQueryStrategy> all() {
        return strategies;
    }

    public List<StationProximity> within(double lat, double lon, double radiusM) {
        return active().within(lat, lon, radiusM);
    }

    public Optional<StationProximity> nearest(double lat, double lon, double radiusM) {
        return active().nearest(lat, lon, radiusM);
    }
}
