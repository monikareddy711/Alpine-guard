import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Bulletin,
  HazardAssessment,
  RouteDetail,
  RouteSummary,
  Station,
  Telemetry
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  stations(): Observable<Station[]> {
    return this.http.get<Station[]>(`${this.base}/stations`);
  }

  telemetry(stationId: string, hours: number): Observable<Telemetry> {
    const params = new HttpParams().set('hours', hours);
    return this.http.get<Telemetry>(`${this.base}/stations/${stationId}/telemetry`, { params });
  }

  routes(windowHours: number): Observable<RouteSummary[]> {
    const params = new HttpParams().set('windowHours', windowHours);
    return this.http.get<RouteSummary[]>(`${this.base}/routes`, { params });
  }

  route(slug: string): Observable<RouteDetail> {
    return this.http.get<RouteDetail>(`${this.base}/routes/${slug}`);
  }

  assessment(slug: string, windowHours: number): Observable<HazardAssessment> {
    const params = new HttpParams().set('windowHours', windowHours);
    return this.http.get<HazardAssessment>(`${this.base}/routes/${slug}/hazard-assessment`, { params });
  }

  bulletins(): Observable<Bulletin[]> {
    return this.http.get<Bulletin[]>(`${this.base}/bulletins`);
  }
}
