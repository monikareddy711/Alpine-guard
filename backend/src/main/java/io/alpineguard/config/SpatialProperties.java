package io.alpineguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alpineguard.spatial")
public class SpatialProperties {

    private String defaultStrategy = "kd-tree";
    private double proximityRadiusM = 5000.0;
    private int stationDatasetSize = 250;

    public String getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(String defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public double getProximityRadiusM() {
        return proximityRadiusM;
    }

    public void setProximityRadiusM(double proximityRadiusM) {
        this.proximityRadiusM = proximityRadiusM;
    }

    public int getStationDatasetSize() {
        return stationDatasetSize;
    }

    public void setStationDatasetSize(int stationDatasetSize) {
        this.stationDatasetSize = stationDatasetSize;
    }
}
