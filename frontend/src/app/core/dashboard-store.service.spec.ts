import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardStore } from './dashboard-store.service';
import { HazardAssessment, RouteDetail, RouteSummary, Station } from './models';

function station(id: string, name: string): Station {
  return {
    stationId: id,
    name,
    region: 'AT-07 Tyrol',
    operator: 'Land Tirol',
    network: 'LINEA',
    elevationM: 2600,
    elevationBand: 'MID',
    lat: 46.98,
    lon: 11.1,
    active: true,
    dangerRating: 3,
    severity: 'CONSIDERABLE',
    latestReading: null
  };
}

describe('DashboardStore', () => {
  let store: DashboardStore;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DashboardStore, provideHttpClient(), provideHttpClientTesting()]
    });
    store = TestBed.inject(DashboardStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads stations, routes and bulletins', () => {
    store.load();
    http.expectOne('/api/v1/stations').flush([station('AT07-114', 'Stubai Eisjoch')]);
    http.expectOne('/api/v1/routes?windowHours=72').flush([]);
    http.expectOne('/api/v1/bulletins').flush([]);

    let names: string[] = [];
    store.stations$.subscribe((items) => (names = items.map((item) => item.name)));
    expect(names).toEqual(['Stubai Eisjoch']);
  });

  it('publishes a camera target when a station is selected', () => {
    let target: { lat: number; lon: number } | null = null;
    store.focus$.subscribe((value) => (target = value));

    store.load();
    http.expectOne('/api/v1/stations').flush([station('AT07-103', 'Kuehtai')]);
    http.expectOne('/api/v1/routes?windowHours=72').flush([]);
    http.expectOne('/api/v1/bulletins').flush([]);

    expect(target).not.toBeNull();
    expect(target!.lat).toBeCloseTo(46.98, 5);
  });

  it('propagates a selection to every subscriber in under 100 ms', () => {
    store.load();
    http.expectOne('/api/v1/stations').flush([station('AT07-114', 'Stubai Eisjoch'), station('CHGR-201', 'Weissfluhjoch')]);
    http.expectOne('/api/v1/routes?windowHours=72').flush([]);
    http.expectOne('/api/v1/bulletins').flush([]);

    const observed: string[] = [];
    store.selectedStation$.subscribe((id) => {
      if (id) {
        observed.push(id);
      }
    });
    const started = performance.now();
    store.selectStation('CHGR-201');
    const elapsed = performance.now() - started;

    expect(observed).toContain('CHGR-201');
    expect(elapsed).toBeLessThan(100);
  });

  it('requests telemetry for the selected station and window', () => {
    store.selectStation('AT07-103');
    store.telemetry$.subscribe();
    const request = http.expectOne('/api/v1/stations/AT07-103/telemetry?hours=72');
    request.flush({
      stationId: 'AT07-103',
      name: 'Kuehtai',
      hours: 72,
      from: '2026-03-13T00:00:00Z',
      to: '2026-03-16T00:00:00Z',
      timestamps: [],
      airTempC: [],
      windSpeedMs: [],
      windGustMs: [],
      windDirDeg: [],
      snowDepthCm: [],
      newSnowCm: []
    });
    expect(true).toBeTrue();
  });

  it('refetches the assessment when the time window changes', () => {
    store.selectRoute('sulzkogel-kuehtai');
    http.expectOne('/api/v1/routes/sulzkogel-kuehtai').flush({
      slug: 'sulzkogel-kuehtai',
      name: 'Kuehtai to Sulzkogel',
      region: 'AT-07 Tyrol',
      difficulty: 'D4',
      distanceM: 1000,
      ascentM: 500,
      descentM: 0,
      maxElevationM: 2800,
      minElevationM: 2300,
      points: [],
      geoJson: { type: 'FeatureCollection', features: [] }
    } as RouteDetail);

    store.assessment$.subscribe();
    http.expectOne('/api/v1/routes/sulzkogel-kuehtai/hazard-assessment?windowHours=72')
      .flush({ segments: [] } as unknown as HazardAssessment);

    store.setWindowHours(24);
    http.expectOne('/api/v1/routes?windowHours=24').flush([] as RouteSummary[]);
    http.expectOne('/api/v1/routes/sulzkogel-kuehtai/hazard-assessment?windowHours=24')
      .flush({ segments: [] } as unknown as HazardAssessment);

    expect(store.currentWindowHours()).toBe(24);
  });

  it('emits a hover target for a profile point', () => {
    const targets: string[] = [];
    store.focus$.subscribe((target) => targets.push(target.label));
    store.hoverPoint({ seq: 4, lat: 47.1, lon: 11.0, elevationM: 2400, distanceM: 640, slopeDeg: 30, aspectDeg: 170 });
    expect(targets[targets.length - 1]).toContain('640');
  });
});
