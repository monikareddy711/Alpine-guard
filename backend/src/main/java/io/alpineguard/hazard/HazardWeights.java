package io.alpineguard.hazard;

public record HazardWeights(double slope, double wind, double snow, double danger, double temp) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private double slope;
        private double wind;
        private double snow;
        private double danger;
        private double temp;

        public Builder slope(double value) {
            this.slope = value;
            return this;
        }

        public Builder wind(double value) {
            this.wind = value;
            return this;
        }

        public Builder snow(double value) {
            this.snow = value;
            return this;
        }

        public Builder danger(double value) {
            this.danger = value;
            return this;
        }

        public Builder temp(double value) {
            this.temp = value;
            return this;
        }

        public HazardWeights build() {
            double sum = slope + wind + snow + danger + temp;
            if (Math.abs(sum - 1.0) > 1e-6) {
                throw new IllegalStateException("hazard weights must sum to 1.0, got " + sum);
            }
            return new HazardWeights(slope, wind, snow, danger, temp);
        }
    }
}
