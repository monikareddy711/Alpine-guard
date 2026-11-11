package io.alpineguard.api.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Assembles GeoJSON envelopes so controllers never build nested maps by hand. */
public final class GeoJsonFeatureCollectionBuilder {

    private final List<Map<String, Object>> features = new ArrayList<>();

    public static GeoJsonFeatureCollectionBuilder create() {
        return new GeoJsonFeatureCollectionBuilder();
    }

    public GeoJsonFeatureCollectionBuilder point(double lon, double lat, Map<String, Object> properties) {
        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "Point");
        geometry.put("coordinates", List.of(lon, lat));
        features.add(feature(geometry, properties));
        return this;
    }

    public GeoJsonFeatureCollectionBuilder line(List<List<Double>> coordinates, Map<String, Object> properties) {
        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "LineString");
        geometry.put("coordinates", coordinates);
        features.add(feature(geometry, properties));
        return this;
    }

    private Map<String, Object> feature(Map<String, Object> geometry, Map<String, Object> properties) {
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", geometry);
        feature.put("properties", properties);
        return feature;
    }

    public Map<String, Object> build() {
        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", List.copyOf(features));
        return collection;
    }
}
