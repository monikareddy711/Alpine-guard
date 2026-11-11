import { WGS84_A, cameraDestination, ecefFromGeodetic, segmentPositions, stationMarkerPosition } from './cesium-geo';
import { SegmentHazard, Station } from './models';

describe('cesium coordinate conversion', () => {
  it('places the equator and prime meridian on the x axis', () => {
    const [x, y, z] = ecefFromGeodetic(0, 0, 0);
    expect(x).toBeCloseTo(WGS84_A, 3);
    expect(y).toBeCloseTo(0, 6);
    expect(z).toBeCloseTo(0, 6);
  });

  it('places the north pole on the z axis at the polar radius', () => {
    const [x, y, z] = ecefFromGeodetic(90, 0, 0);
    expect(x).toBeCloseTo(0, 3);
    expect(y).toBeCloseTo(0, 3);
    expect(z).toBeCloseTo(6356752.3142, 1);
  });

  it('adds the height above the ellipsoid', () => {
    const ground = ecefFromGeodetic(46.83, 9.81, 0);
    const raised = ecefFromGeodetic(46.83, 9.81, 1000);
    const delta = Math.hypot(raised[0] - ground[0], raised[1] - ground[1], raised[2] - ground[2]);
    expect(delta).toBeCloseTo(1000, 1);
  });

  it('lifts station markers 60 m above the station elevation', () => {
    const station: Station = {
      stationId: 'CHGR-201',
      name: 'Weissfluhjoch',
      region: 'CH-GR Graubuenden',
      operator: 'SLF',
      network: 'IMIS',
      elevationM: 2693,
      elevationBand: 'MID',
      lat: 46.8333,
      lon: 9.8067,
      active: true,
      dangerRating: 3,
      severity: 'CONSIDERABLE',
      latestReading: null
    };
    const marker = stationMarkerPosition(station);
    const reference = ecefFromGeodetic(46.8333, 9.8067, 2753);
    expect(marker[0]).toBeCloseTo(reference[0], 6);
    expect(marker[2]).toBeCloseTo(reference[2], 6);
  });

  it('builds two positions per route segment', () => {
    const segment: SegmentHazard = {
      index: 3,
      startLat: 47.19,
      startLon: 11.03,
      endLat: 47.191,
      endLon: 11.031,
      elevationM: 2400,
      slopeDeg: 32,
      aspectDeg: 180,
      score: 41,
      severity: 'MODERATE',
      band: 'MID',
      nearestStationId: 'AT07-103',
      nearestStationName: 'Kuehtai',
      nearestStationDistanceM: 1200,
      leeAlignment: 0.7,
      flags: ['STEEP_SLOPE']
    };
    const positions = segmentPositions(segment);
    expect(positions.length).toBe(2);
    expect(positions[0][2]).toBeGreaterThan(positions[1][2] - 100);
  });

  it('returns the camera destination for a lat lon height triple', () => {
    expect(cameraDestination(46.98, 11.1, 9000)).toEqual(ecefFromGeodetic(46.98, 11.1, 9000));
  });
});
