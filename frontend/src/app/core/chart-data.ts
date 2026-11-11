import { PlotlyTrace } from 'plotly.js-dist-min';
import { HazardAssessment, RouteDetail, Telemetry } from './models';
import { severityColor } from './severity';

/** Telemetry series: snow depth on the left axis, gust bars and temperature on the right. */
export function buildTelemetryTraces(telemetry: Telemetry): PlotlyTrace[] {
  const timestamps = telemetry.timestamps.map((stamp) => new Date(stamp));
  return [
    {
      type: 'scatter',
      mode: 'lines',
      name: 'snow depth (cm)',
      x: timestamps,
      y: telemetry.snowDepthCm,
      line: { color: '#4aa3df', width: 2 },
      yaxis: 'y'
    },
    {
      type: 'bar',
      name: 'gust (m/s)',
      x: timestamps,
      y: telemetry.windGustMs,
      marker: { color: 'rgba(224, 139, 60, 0.55)' },
      yaxis: 'y2'
    },
    {
      type: 'scatter',
      mode: 'lines',
      name: 'air temp (C)',
      x: timestamps,
      y: telemetry.airTempC,
      line: { color: '#d8c34a', width: 1.6, dash: 'dot' },
      yaxis: 'y3'
    }
  ];
}

/** Route profile: elevation line plus one slope bar per assessed segment, coloured by severity. */
export function buildProfileTraces(route: RouteDetail, assessment: HazardAssessment | null): PlotlyTrace[] {
  const points = route.points;
  const distanceKm = points.map((point) => point.distanceM / 1000);
  const elevation = points.map((point) => point.elevationM);
  const segments = assessment?.segments ?? [];
  return [
    {
      type: 'scatter',
      mode: 'lines',
      name: 'elevation (m)',
      x: distanceKm,
      y: elevation,
      line: { color: '#4aa3df', width: 2 },
      fill: 'tozeroy',
      fillcolor: 'rgba(74, 163, 223, 0.12)',
      yaxis: 'y'
    },
    {
      type: 'bar',
      name: 'slope (deg)',
      x: segments.map((segment) => distanceKm[Math.min(segment.index, distanceKm.length - 1)]),
      y: segments.map((segment) => segment.slopeDeg),
      marker: { color: segments.map((segment) => severityColor(segment.severity)) },
      yaxis: 'y2'
    }
  ];
}
