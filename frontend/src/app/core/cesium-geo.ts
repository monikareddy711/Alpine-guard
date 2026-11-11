import { RoutePoint, SegmentHazard, Station } from './models';

/** WGS84 semi major axis and first eccentricity squared, the constants Cesium uses. */
export const WGS84_A = 6378137.0;
export const WGS84_E2 = 6.69437999014e-3;

/**
 * Geodetic to earth centred earth fixed conversion, the same projection Cesium applies to
 * Cartesian3.fromDegrees. Keeping it here lets the placement maths be unit tested without
 * loading the whole Cesium bundle in the browser test runner.
 */
export function ecefFromGeodetic(latDeg: number, lonDeg: number, heightM: number): [number, number, number] {
  const lat = (latDeg * Math.PI) / 180;
  const lon = (lonDeg * Math.PI) / 180;
  const sinLat = Math.sin(lat);
  const n = WGS84_A / Math.sqrt(1 - WGS84_E2 * sinLat * sinLat);
  const x = (n + heightM) * Math.cos(lat) * Math.cos(lon);
  const y = (n + heightM) * Math.cos(lat) * Math.sin(lon);
  const z = (n * (1 - WGS84_E2) + heightM) * sinLat;
  return [x, y, z];
}

/** Station markers sit a little above the terrain so the pin is not buried in the mesh. */
export function stationMarkerPosition(station: Station): [number, number, number] {
  return ecefFromGeodetic(station.lat, station.lon, station.elevationM + 60);
}

export function segmentPositions(segment: SegmentHazard): [number, number, number][] {
  return [
    ecefFromGeodetic(segment.startLat, segment.startLon, segment.elevationM + 8),
    ecefFromGeodetic(segment.endLat, segment.endLon, segment.elevationM + 8)
  ];
}

/** Position of an arbitrary lat/lon/elevation triple. */
export function stationPositionOf(lat: number, lon: number, elevationM: number): [number, number, number] {
  return ecefFromGeodetic(lat, lon, elevationM);
}

export function profilePointPosition(point: RoutePoint): [number, number, number] {
  return ecefFromGeodetic(point.lat, point.lon, point.elevationM);
}

export function cameraDestination(latDeg: number, lonDeg: number, heightM: number): [number, number, number] {
  return ecefFromGeodetic(latDeg, lonDeg, heightM);
}
