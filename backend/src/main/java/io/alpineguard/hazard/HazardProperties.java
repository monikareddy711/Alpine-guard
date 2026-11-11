package io.alpineguard.hazard;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alpineguard.hazard")
public class HazardProperties {

    private double steepSlopeDeg = 30.0;
    private double leeToleranceDeg = 45.0;
    private double windLoadingGustMs = 12.0;

    public double getSteepSlopeDeg() {
        return steepSlopeDeg;
    }

    public void setSteepSlopeDeg(double steepSlopeDeg) {
        this.steepSlopeDeg = steepSlopeDeg;
    }

    public double getLeeToleranceDeg() {
        return leeToleranceDeg;
    }

    public void setLeeToleranceDeg(double leeToleranceDeg) {
        this.leeToleranceDeg = leeToleranceDeg;
    }

    public double getWindLoadingGustMs() {
        return windLoadingGustMs;
    }

    public void setWindLoadingGustMs(double windLoadingGustMs) {
        this.windLoadingGustMs = windLoadingGustMs;
    }
}
