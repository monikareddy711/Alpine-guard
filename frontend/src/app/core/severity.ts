import { HazardSeverity } from './models';

export const SEVERITY_COLORS: Record<HazardSeverity, string> = {
  LOW: '#4caf7d',
  MODERATE: '#d8c34a',
  CONSIDERABLE: '#e08b3c',
  HIGH: '#d9534f',
  EXTREME: '#8e44ad'
};

export const DANGER_LABELS: Record<number, string> = {
  1: 'low',
  2: 'moderate',
  3: 'considerable',
  4: 'high',
  5: 'very high'
};

export function severityColor(severity: HazardSeverity): string {
  return SEVERITY_COLORS[severity] ?? SEVERITY_COLORS.LOW;
}

/** Slope steepness ramp used for the 3D route polylines. */
export function slopeColor(slopeDeg: number): string {
  if (slopeDeg < 25) {
    return '#3f7fbf';
  }
  if (slopeDeg < 30) {
    return '#4caf7d';
  }
  if (slopeDeg < 35) {
    return '#d8c34a';
  }
  if (slopeDeg < 40) {
    return '#e08b3c';
  }
  return '#d9534f';
}
