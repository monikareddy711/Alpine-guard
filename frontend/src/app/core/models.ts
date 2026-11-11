export type HazardSeverity = 'LOW' | 'MODERATE' | 'CONSIDERABLE' | 'HIGH' | 'EXTREME';
export type ElevationBand = 'LOW' | 'MID' | 'HIGH';

export interface Reading {
  measuredAt: string;
  airTempC: number | null;
  windSpeedMs: number | null;
  windGustMs: number | null;
  windDirDeg: number | null;
  snowDepthCm: number | null;
  newSnowCm: number | null;
  humidityPct: number | null;
  pressureHpa: number | null;
}

export interface Station {
  stationId: string;
  name: string;
  region: string;
  operator: string;
  network: string;
  elevationM: number;
  elevationBand: ElevationBand;
  lat: number;
  lon: number;
  active: boolean;
  dangerRating: number | null;
  severity: HazardSeverity;
  latestReading: Reading | null;
}

export interface Telemetry {
  stationId: string;
  name: string;
  hours: number;
  from: string;
  to: string;
  timestamps: string[];
  airTempC: (number | null)[];
  windSpeedMs: (number | null)[];
  windGustMs: (number | null)[];
  windDirDeg: (number | null)[];
  snowDepthCm: (number | null)[];
  newSnowCm: (number | null)[];
}

export interface RouteSummary {
  slug: string;
  name: string;
  region: string;
  difficulty: string;
  distanceM: number;
  ascentM: number;
  descentM: number;
  maxElevationM: number;
  minElevationM: number;
  maxScore: number;
  meanScore: number;
  severity: HazardSeverity;
  maxSlopeDeg: number;
  steepSegmentCount: number;
  totalSegments: number;
}

export interface RoutePoint {
  seq: number;
  lat: number;
  lon: number;
  elevationM: number;
  distanceM: number;
  slopeDeg: number;
  aspectDeg: number;
}

export interface GeoJsonLine {
  type: 'FeatureCollection';
  features: Array<{
    type: 'Feature';
    geometry: { type: 'LineString'; coordinates: number[][] };
    properties: Record<string, unknown>;
  }>;
}

export interface RouteDetail {
  slug: string;
  name: string;
  region: string;
  difficulty: string;
  distanceM: number;
  ascentM: number;
  descentM: number;
  maxElevationM: number;
  minElevationM: number;
  points: RoutePoint[];
  geoJson: GeoJsonLine;
}

export interface SegmentHazard {
  index: number;
  startLat: number;
  startLon: number;
  endLat: number;
  endLon: number;
  elevationM: number;
  slopeDeg: number;
  aspectDeg: number;
  score: number;
  severity: HazardSeverity;
  band: ElevationBand;
  nearestStationId: string | null;
  nearestStationName: string | null;
  nearestStationDistanceM: number;
  leeAlignment: number;
  flags: string[];
}

export interface HazardAssessment {
  routeSlug: string;
  routeName: string;
  region: string;
  generatedAt: string;
  windowHours: number;
  maxScore: number;
  meanScore: number;
  severity: string;
  maxSlopeDeg: number;
  steepSegmentCount: number;
  totalSegments: number;
  segments: SegmentHazard[];
  geoJson: GeoJsonLine;
}

export interface Bulletin {
  region: string;
  validDate: string;
  elevationBand: ElevationBand;
  dangerRating: number;
  mainProblem: string;
  summary: string;
  issuedAt: string;
}

export interface StationFilter {
  region: string | null;
  band: ElevationBand | null;
  minDanger: number | null;
  text: string;
}
