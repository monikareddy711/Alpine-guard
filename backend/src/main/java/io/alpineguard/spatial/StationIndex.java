package io.alpineguard.spatial;

import io.alpineguard.domain.Station;
import io.alpineguard.repository.StationRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/** Holds the station snapshot and its k-d tree. Rebuilt when the station set changes. */
@Component
public class StationIndex {

    private final StationRepository stationRepository;
    private volatile Snapshot snapshot = new Snapshot(List.of(), new KdTree(new double[0], new double[0]));

    public StationIndex(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public record Snapshot(List<Station> stations, KdTree tree) {
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        refresh();
    }

    public void refresh() {
        List<Station> stations = stationRepository.findAllActive();
        double[] lats = new double[stations.size()];
        double[] lons = new double[stations.size()];
        for (int i = 0; i < stations.size(); i++) {
            lats[i] = stations.get(i).lat();
            lons[i] = stations.get(i).lon();
        }
        snapshot = new Snapshot(List.copyOf(stations), new KdTree(lats, lons));
    }

    public Snapshot snapshot() {
        return snapshot;
    }
}
