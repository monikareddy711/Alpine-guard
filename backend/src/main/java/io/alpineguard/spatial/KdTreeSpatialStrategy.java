package io.alpineguard.spatial;

import io.alpineguard.domain.GeoMath;
import io.alpineguard.domain.Station;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class KdTreeSpatialStrategy implements SpatialQueryStrategy {

    private final StationIndex index;

    public KdTreeSpatialStrategy(StationIndex index) {
        this.index = index;
    }

    @Override
    public String name() {
        return "kd-tree";
    }

    @Override
    public List<StationProximity> within(double lat, double lon, double radiusM) {
        StationIndex.Snapshot snap = index.snapshot();
        List<Station> stations = snap.stations();
        List<StationProximity> hits = new ArrayList<>();
        for (int i : snap.tree().within(lat, lon, radiusM)) {
            Station station = stations.get(i);
            double distance = GeoMath.haversineM(lat, lon, station.lat(), station.lon());
            if (distance <= radiusM) {
                hits.add(new StationProximity(station, distance));
            }
        }
        hits.sort(Comparator.comparingDouble(StationProximity::distanceM));
        return hits;
    }

    @Override
    public Optional<StationProximity> nearest(double lat, double lon, double radiusM) {
        StationIndex.Snapshot snap = index.snapshot();
        int candidate = snap.tree().nearest(lat, lon);
        if (candidate < 0) {
            return Optional.empty();
        }
        Station station = snap.stations().get(candidate);
        double distance = GeoMath.haversineM(lat, lon, station.lat(), station.lon());
        return distance <= radiusM ? Optional.of(new StationProximity(station, distance)) : Optional.empty();
    }
}
