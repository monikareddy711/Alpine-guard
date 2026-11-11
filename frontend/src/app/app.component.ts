import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TagModule } from 'primeng/tag';
import { DashboardStore } from './core/dashboard-store.service';
import { Bulletin, SegmentHazard } from './core/models';
import { HazardPanelComponent } from './components/hazard-panel.component';
import { RouteProfileComponent } from './components/route-profile.component';
import { StationTableComponent } from './components/station-table.component';
import { TelemetryChartComponent } from './components/telemetry-chart.component';
import { TerrainGlobeComponent } from './components/terrain-globe.component';

@Component({
  selector: 'ag-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DropdownModule,
    SelectButtonModule,
    TagModule,
    TerrainGlobeComponent,
    StationTableComponent,
    TelemetryChartComponent,
    RouteProfileComponent,
    HazardPanelComponent
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  readonly store = inject(DashboardStore);

  stations$ = this.store.stations$;
  routes$ = this.store.routes$;
  bulletins$ = this.store.bulletins$;
  telemetry$ = this.store.telemetry$;
  routeDetail$ = this.store.routeDetail$;
  assessment$ = this.store.assessment$;
  selectedRoute$ = this.store.selectedRoute$;
  windowHours$ = this.store.windowHours$;

  windowOptions = [
    { label: '24 h', value: 24 },
    { label: '48 h', value: 48 },
    { label: '72 h', value: 72 }
  ];

  ngOnInit(): void {
    this.store.load();
  }

  selectRoute(slug: string): void {
    this.store.selectRoute(slug);
  }

  setWindow(hours: number): void {
    this.store.setWindowHours(hours);
  }

  onSegmentSelected(segment: SegmentHazard): void {
    this.store.focusOn(
      (segment.startLat + segment.endLat) / 2,
      (segment.startLon + segment.endLon) / 2,
      2200,
      `segment ${segment.index} · ${segment.slopeDeg} deg`
    );
  }

  bulletinHeadline(bulletins: Bulletin[] | null): string {
    if (!bulletins || bulletins.length === 0) {
      return 'no bulletin';
    }
    const worst = bulletins.reduce((best, item) => (item.dangerRating > best.dangerRating ? item : best), bulletins[0]);
    return `${worst.region} · danger ${worst.dangerRating} · ${worst.mainProblem ?? ''}`.trim();
  }
}
