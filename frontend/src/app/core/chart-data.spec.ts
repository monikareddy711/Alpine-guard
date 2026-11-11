import { buildProfileTraces, buildTelemetryTraces } from './chart-data';
import { HazardAssessment, RouteDetail, Telemetry } from './models';

describe('plotly series transformation', () => {
  const telemetry: Telemetry = {
    stationId: 'AT07-103',
    name: 'Kuehtai',
    hours: 3,
    from: '2026-03-16T00:00:00Z',
    to: '2026-03-16T03:00:00Z',
    timestamps: ['2026-03-16T00:00:00Z', '2026-03-16T01:00:00Z', '2026-03-16T02:00:00Z'],
    airTempC: [-3, -4, -5],
    windSpeedMs: [3, 4, 5],
    windGustMs: [6, 8, 9],
    windDirDeg: [300, 310, 320],
    snowDepthCm: [140, 142, 143],
    newSnowCm: [0, 2, 1]
  };

  it('maps timestamps to date objects on the x axis', () => {
    const traces = buildTelemetryTraces(telemetry);
    expect(traces.length).toBe(3);
    const x = traces[0].x as Date[];
    expect(x.length).toBe(3);
    expect(x[0] instanceof Date).toBeTrue();
    expect((x[1] as Date).getUTCHours()).toBe(1);
  });

  it('keeps snow depth on the primary axis and gust on the secondary axis', () => {
    const traces = buildTelemetryTraces(telemetry);
    expect(traces[0].yaxis).toBe('y');
    expect(traces[1].yaxis).toBe('y2');
    expect(traces[2].yaxis).toBe('y3');
    expect(traces[1].y as number[]).toEqual([6, 8, 9]);
  });

  const route: RouteDetail = {
    slug: 'sulzkogel-kuehtai',
    name: 'Kuehtai to Sulzkogel',
    region: 'AT-07 Tyrol',
    difficulty: 'D4',
    distanceM: 2000,
    ascentM: 500,
    descentM: 20,
    maxElevationM: 2834,
    minElevationM: 2292,
    points: [
      { seq: 0, lat: 47.19, lon: 11.03, elevationM: 2292, distanceM: 0, slopeDeg: 12, aspectDeg: 180 },
      { seq: 1, lat: 47.19, lon: 11.031, elevationM: 2500, distanceM: 1000, slopeDeg: 31, aspectDeg: 190 },
      { seq: 2, lat: 47.19, lon: 11.032, elevationM: 2834, distanceM: 2000, slopeDeg: 38, aspectDeg: 200 }
    ],
    geoJson: { type: 'FeatureCollection', features: [] }
  };

  const assessment = {
    segments: [
      { index: 0, slopeDeg: 12, severity: 'LOW' },
      { index: 1, slopeDeg: 38, severity: 'HIGH' }
    ]
  } as unknown as HazardAssessment;

  it('converts route distances to kilometres for the elevation profile', () => {
    const traces = buildProfileTraces(route, assessment);
    expect(traces[0].x as number[]).toEqual([0, 1, 2]);
    expect(traces[0].y as number[]).toEqual([2292, 2500, 2834]);
  });

  it('colours slope bars by the segment severity', () => {
    const traces = buildProfileTraces(route, assessment);
    const colours = (traces[1].marker as { color: string[] }).color;
    expect(colours.length).toBe(2);
    expect(colours[1]).toBe('#d9534f');
    expect(colours[0]).toBe('#4caf7d');
  });

  it('produces elevation only when no assessment is loaded', () => {
    const traces = buildProfileTraces(route, null);
    expect((traces[1].y as number[]).length).toBe(0);
  });
});
