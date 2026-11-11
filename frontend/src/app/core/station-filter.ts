import { Station, StationFilter } from './models';

export const DEFAULT_FILTER: StationFilter = { region: null, band: null, minDanger: null, text: '' };

/** Pure filter used by the station browser table and by the unit tests. */
export function filterStations(stations: Station[], filter: StationFilter): Station[] {
  const text = filter.text.trim().toLowerCase();
  return stations.filter((station) => {
    if (filter.region && station.region !== filter.region) {
      return false;
    }
    if (filter.band && station.elevationBand !== filter.band) {
      return false;
    }
    if (filter.minDanger !== null && (station.dangerRating ?? 0) < filter.minDanger) {
      return false;
    }
    if (text && !`${station.name} ${station.stationId} ${station.operator}`.toLowerCase().includes(text)) {
      return false;
    }
    return true;
  });
}

export function regionsOf(stations: Station[]): string[] {
  return Array.from(new Set(stations.map((station) => station.region))).sort();
}
