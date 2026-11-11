import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, Input, NgZone, OnDestroy, ViewChild, inject } from '@angular/core';
import Plotly, { PlotlyConfig, PlotlyEvent, PlotlyLayout } from 'plotly.js-dist-min';
import { DashboardStore } from '../core/dashboard-store.service';
import { buildProfileTraces } from '../core/chart-data';
import { HazardAssessment, RouteDetail } from '../core/models';

const AXIS = {
  gridcolor: '#26313d',
  zerolinecolor: '#26313d',
  tickfont: { color: '#8b98a8', size: 10 }
};

@Component({
  selector: 'ag-route-profile',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="ag-panel chart-panel">
      <div class="ag-panel-title">
        <span>{{ title }}</span>
        <span class="hint">hover to fly the 3D camera to that point</span>
      </div>
      <div #chart class="chart"></div>
    </section>
  `,
  styles: [
    `
      .chart-panel {
        display: flex;
        flex-direction: column;
        height: 100%;
      }
      .chart {
        flex: 1;
        min-height: 0;
      }
      .hint {
        text-transform: none;
        letter-spacing: 0;
        font-size: 11px;
      }
    `
  ]
})
export class RouteProfileComponent implements AfterViewInit, OnDestroy {
  @ViewChild('chart', { static: true }) chartRef!: ElementRef<HTMLDivElement>;

  @Input() set route(value: RouteDetail | null) {
    this.routeData = value;
    this.draw();
  }
  @Input() set assessment(value: HazardAssessment | null) {
    this.assessmentData = value;
    this.draw();
  }

  private readonly store = inject(DashboardStore);
  private readonly zone = inject(NgZone);
  private routeData: RouteDetail | null = null;
  private assessmentData: HazardAssessment | null = null;
  private rendered = false;
  private hoverBound = false;

  get title(): string {
    return this.routeData ? `${this.routeData.name} · elevation and slope profile` : 'route profile';
  }

  ngAfterViewInit(): void {
    this.draw();
  }

  ngOnDestroy(): void {
    if (this.chartRef) {
      Plotly.purge(this.chartRef.nativeElement);
    }
  }

  private draw(): void {
    const element = this.chartRef?.nativeElement;
    if (!element || !this.routeData) {
      return;
    }
    const traces = buildProfileTraces(this.routeData, this.assessmentData);
    const layout: PlotlyLayout = {
      paper_bgcolor: 'rgba(0,0,0,0)',
      plot_bgcolor: 'rgba(0,0,0,0)',
      margin: { l: 46, r: 42, t: 8, b: 30 },
      font: { color: '#8b98a8', size: 10 },
      legend: { orientation: 'h', x: 0, y: 1.14, font: { size: 10 } },
      bargap: 0.05,
      xaxis: { ...AXIS, title: { text: 'distance (km)', font: { color: '#8b98a8', size: 10 } } },
      yaxis: { ...AXIS, title: { text: 'elevation m', font: { color: '#4aa3df', size: 10 } } },
      yaxis2: {
        ...AXIS,
        overlaying: 'y',
        side: 'right',
        showgrid: false,
        range: [0, 55],
        title: { text: 'slope deg', font: { color: '#e08b3c', size: 10 } }
      }
    };
    const config: PlotlyConfig = { displayModeBar: false, responsive: true };
    if (this.rendered) {
      void Plotly.react(element, traces, layout, config);
    } else {
      void Plotly.newPlot(element, traces, layout, config);
      this.rendered = true;
    }
    if (!this.hoverBound) {
      this.hoverBound = true;
      this.bindHover(element);
    }
  }

  private bindHover(element: HTMLElement): void {
    const target = element as HTMLElement & {
      on?: (event: string, handler: (payload: PlotlyEvent) => void) => void;
    };
    target.on?.('plotly_hover', (payload) => {
      const index = payload.points?.[0]?.pointIndex ?? null;
      const points = this.routeData?.points ?? [];
      if (index === null || index < 0 || index >= points.length) {
        return;
      }
      const point = points[index];
      this.zone.run(() => this.store.hoverPoint(point));
    });
  }
}
