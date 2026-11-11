import { Station } from './models';
import { filterStations, regionsOf } from './station-filter';

function station(overrides: Partial<Station>): Station {
  return {
    stationId: 'AT07-101',
    name: 'Patscherkofel',
    region: 'AT-07 Tyrol',
    operator: 'Land Tirol',
    network: 'LINEA',
    elevationM: 2247,
    elevationBand: 'MID',
    lat: 47.2094,
    lon: 11.4606,
    active: true,
    dangerRating: 3,
    severity: 'CONSIDERABLE',
    latestReading: null,
    ...overrides
  };
}

describe('station filtering', () => {
  const stations = [
    station({ stationId: 'AT07-101', name: 'Patscherkofel', elevationM: 2247, elevationBand: 'MID', dangerRating: 3 }),
    station({
      stationId: 'AT07-111',
      name: 'Zugspitze',
      elevationM: 2938,
      elevationBand: 'HIGH',
      dangerRating: 4,
      severity: 'HIGH'
    }),
    station({
      stationId: 'CHGR-205',
      name: 'Samedan',
      region: 'CH-GR Graubuenden',
      operator: 'SLF',
      network: 'IMIS',
      elevationM: 1700,
      elevationBand: 'LOW',
      dangerRating: 2,
      severity: 'MODERATE'
    })
  ];

  it('returns every station for the default filter', () => {
    expect(filterStations(stations, { region: null, band: null, minDanger: null, text: '' }).length).toBe(3);
  });

  it('filters by region', () => {
    const result = filterStations(stations, { region: 'CH-GR Graubuenden', band: null, minDanger: null, text: '' });
    expect(result.map((item) => item.stationId)).toEqual(['CHGR-205']);
  });

  it('filters by elevation band', () => {
    const result = filterStations(stations, { region: null, band: 'HIGH', minDanger: null, text: '' });
    expect(result.map((item) => item.stationId)).toEqual(['AT07-111']);
  });

  it('filters by minimum danger rating', () => {
    const result = filterStations(stations, { region: null, band: null, minDanger: 4, text: '' });
    expect(result.map((item) => item.stationId)).toEqual(['AT07-111']);
  });

  it('treats a station without a rating as below any threshold', () => {
    const withoutRating = station({ stationId: 'AT07-999', dangerRating: null });
    expect(filterStations([withoutRating], { region: null, band: null, minDanger: 2, text: '' })).toEqual([]);
  });

  it('matches the free text against name, id and operator', () => {
    expect(filterStations(stations, { region: null, band: null, minDanger: null, text: 'slf' }).length).toBe(1);
    expect(filterStations(stations, { region: null, band: null, minDanger: null, text: 'zugsp' }).length).toBe(1);
    expect(filterStations(stations, { region: null, band: null, minDanger: null, text: 'AT07-101' }).length).toBe(1);
  });

  it('lists the distinct regions in order', () => {
    expect(regionsOf(stations)).toEqual(['AT-07 Tyrol', 'CH-GR Graubuenden']);
  });
});
