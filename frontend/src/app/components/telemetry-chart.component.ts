import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import Plotly, { PlotlyConfig, PlotlyLayout } from 'plotly.js-dist-min';
import { buildTelemetryTraces } from '../core/chart-data';
import { Telemetry } from '../core/models';

const AXIS = {
  gridcolor: '#26313d',
  zerolinecolor: '#26313d',
  tickfont: { color: '#8b98a8', size: 10 },
  title: { font: { color: '#8b98a8', size: 11 } }
};

@Component({
  selector: 'ag-telemetry-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="ag-panel chart-panel">
      <div class="ag-panel-title">
        <span>{{ title }}</span>
        <span class="hint">snow depth, wind gust and air temperature</span>
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
export class TelemetryChartComponent implements AfterViewInit, OnDestroy {
  @ViewChild('chart', { static: true }) chartRef!: ElementRef<HTMLDivElement>;

  @Input() set telemetry(value: Telemetry | null) {
    this.data = value;
    this.draw();
  }

  data: Telemetry | null = null;
  private rendered = false;

  get title(): string {
    return this.data ? `${this.data.name} · ${this.data.hours} h telemetry` : 'station telemetry';
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
    if (!element || !this.data) {
      return;
    }
    const traces = buildTelemetryTraces(this.data);
    const layout: PlotlyLayout = {
      paper_bgcolor: 'rgba(0,0,0,0)',
      plot_bgcolor: 'rgba(0,0,0,0)',
      margin: { l: 42, r: 42, t: 8, b: 28 },
      font: { color: '#8b98a8', size: 10 },
      legend: { orientation: 'h', x: 0, y: 1.12, font: { size: 10 } },
      hovermode: 'x unified',
      xaxis: { ...AXIS, type: 'date' },
      yaxis: { ...AXIS, title: { text: 'snow cm', font: { color: '#4aa3df', size: 10 } } },
      yaxis2: {
        ...AXIS,
        overlaying: 'y',
        side: 'right',
        showgrid: false,
        title: { text: 'gust m/s', font: { color: '#e08b3c', size: 10 } }
      },
      yaxis3: {
        ...AXIS,
        overlaying: 'y',
        side: 'right',
        position: 0.94,
        showgrid: false,
        title: { text: 'C', font: { color: '#d8c34a', size: 10 } }
      }
    };
    const config: PlotlyConfig = { displayModeBar: false, responsive: true };
    if (this.rendered) {
      void Plotly.react(element, traces, layout, config);
    } else {
      void Plotly.newPlot(element, traces, layout, config);
      this.rendered = true;
    }
  }
}
