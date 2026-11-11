import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, ReplaySubject, Subject, combineLatest, of, shareReplay, switchMap } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from './api.service';
import {
  Bulletin,
  HazardAssessment,
  RouteDetail,
  RoutePoint,
  RouteSummary,
  Station,
  Telemetry
} from './models';

export interface FocusTarget {
  lat: number;
  lon: number;
  height: number;
  label: string;
}

/**
 * Single reactive source for the dashboard. The PrimeNG table, the Plotly charts and the
 * Cesium viewer all read from these streams, so a selection made in one panel is published
 * to the others in the same tick.
 */
@Injectable({ providedIn: 'root' })
export class DashboardStore {
  private readonly api = inject(ApiService);

  private readonly stationsSubject = new BehaviorSubject<Station[]>([]);
  private readonly routesSubject = new BehaviorSubject<RouteSummary[]>([]);
  private readonly bulletinsSubject = new BehaviorSubject<Bulletin[]>([]);
  private readonly selectedStationSubject = new BehaviorSubject<string | null>(null);
  private readonly selectedRouteSubject = new BehaviorSubject<string | null>(null);
  private readonly windowHoursSubject = new BehaviorSubject<number>(72);
  private readonly hoveredPointSubject = new ReplaySubject<RoutePoint | null>(1);
  private readonly focusSubject = new Subject<FocusTarget>();

  readonly stations$: Observable<Station[]> = this.stationsSubject.asObservable();
  readonly routes$: Observable<RouteSummary[]> = this.routesSubject.asObservable();
  readonly bulletins$: Observable<Bulletin[]> = this.bulletinsSubject.asObservable();
  readonly selectedStation$: Observable<string | null> = this.selectedStationSubject.asObservable();
  readonly selectedRoute$: Observable<string | null> = this.selectedRouteSubject.asObservable();
  readonly windowHours$: Observable<number> = this.windowHoursSubject.asObservable();
  readonly hoveredPoint$: Observable<RoutePoint | null> = this.hoveredPointSubject.asObservable();
  readonly focus$: Observable<FocusTarget> = this.focusSubject.asObservable();

  readonly telemetry$: Observable<Telemetry | null> = combineLatest([
    this.selectedStationSubject,
    this.windowHoursSubject
  ]).pipe(
    switchMap(([stationId, hours]) => (stationId ? this.api.telemetry(stationId, hours) : of(null))),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  readonly routeDetail$: Observable<RouteDetail | null> = this.selectedRouteSubject.pipe(
    switchMap((slug) => (slug ? this.api.route(slug) : of(null))),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  readonly assessment$: Observable<HazardAssessment | null> = combineLatest([
    this.selectedRouteSubject,
    this.windowHoursSubject
  ]).pipe(
    switchMap(([slug, hours]) => (slug ? this.api.assessment(slug, hours) : of(null))),
    shareReplay({ bufferSize: 1, refCount: true })
  );

  readonly selectedStationDetail$: Observable<Station | null> = combineLatest([
    this.stationsSubject,
    this.selectedStationSubject
  ]).pipe(map(([stations, id]) => stations.find((station) => station.stationId === id) ?? null));

  load(): void {
    this.api.stations().subscribe((stations) => {
      this.stationsSubject.next(stations);
      if (!this.selectedStationSubject.value && stations.length > 0) {
        const first = stations.find((station) => station.stationId === 'AT07-114') ?? stations[0];
        this.selectStation(first.stationId);
      }
    });
    this.api.routes(this.windowHoursSubject.value).subscribe((routes) => {
      this.routesSubject.next(routes);
      if (!this.selectedRouteSubject.value && routes.length > 0) {
        const steepest = routes.reduce((best, route) => (route.maxScore > best.maxScore ? route : best), routes[0]);
        this.selectRoute(steepest.slug);
      }
    });
    this.api.bulletins().subscribe((bulletins) => this.bulletinsSubject.next(bulletins));
  }

  selectStation(stationId: string): void {
    this.selectedStationSubject.next(stationId);
    const station = this.stationsSubject.value.find((item) => item.stationId === stationId);
    if (station) {
      this.focusSubject.next({ lat: station.lat, lon: station.lon, height: 6000, label: station.name });
    }
  }

  selectRoute(slug: string): void {
    // The globe frames the whole track itself, so no camera target is published here.
    this.selectedRouteSubject.next(slug);
  }

  setWindowHours(hours: number): void {
    this.windowHoursSubject.next(hours);
    this.api.routes(hours).subscribe((routes) => this.routesSubject.next(routes));
  }

  hoverPoint(point: RoutePoint | null): void {
    this.hoveredPointSubject.next(point);
    if (point) {
      this.focusSubject.next({
        lat: point.lat,
        lon: point.lon,
        height: 3200,
        label: `profile ${point.distanceM} m`
      });
    }
  }

  focusOn(lat: number, lon: number, height: number, label: string): void {
    this.focusSubject.next({ lat, lon, height, label });
  }

  currentStationId(): string | null {
    return this.selectedStationSubject.value;
  }

  currentRouteSlug(): string | null {
    return this.selectedRouteSubject.value;
  }

  currentWindowHours(): number {
    return this.windowHoursSubject.value;
  }
}
