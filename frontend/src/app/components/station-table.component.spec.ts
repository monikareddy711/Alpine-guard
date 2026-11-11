import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { StationTableComponent } from './station-table.component';
import { DashboardStore } from '../core/dashboard-store.service';
import { Station } from '../core/models';

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

describe('StationTableComponent', () => {
  let fixture: ComponentFixture<StationTableComponent>;
  let component: StationTableComponent;

  const stations = [
    station({ stationId: 'AT07-101', name: 'Patscherkofel' }),
    station({ stationId: 'CHGR-201', name: 'Weissfluhjoch', region: 'CH-GR Graubuenden', elevationBand: 'MID' }),
    station({
      stationId: 'AT07-111',
      name: 'Zugspitze',
      elevationM: 2938,
      elevationBand: 'HIGH',
      dangerRating: 4,
      severity: 'HIGH'
    })
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StationTableComponent, NoopAnimationsModule],
      providers: [DashboardStore, provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(StationTableComponent);
    component = fixture.componentInstance;
    component.stations = stations;
    fixture.detectChanges();
  });

  it('renders a row per station', () => {
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(3);
  });

  it('narrows the visible rows when a band is chosen', () => {
    component.filter = { region: null, band: 'HIGH', minDanger: null, text: '' };
    component.apply();
    fixture.detectChanges();
    expect(component.visible.length).toBe(1);
    expect(component.visible[0].name).toBe('Zugspitze');
  });

  it('narrows the visible rows by region and text', () => {
    component.filter = { region: 'CH-GR Graubuenden', minDanger: null, band: null, text: 'weiss' };
    component.apply();
    expect(component.visible.map((item) => item.stationId)).toEqual(['CHGR-201']);
  });

  it('formats the danger rating label', () => {
    expect(component.dangerLabel(4)).toBe('4 high');
    expect(component.dangerLabel(null)).toBe('n/a');
  });

  it('selects a station through the store on row selection', () => {
    const store = TestBed.inject(DashboardStore);
    const selected: (string | null)[] = [];
    store.selectedStation$.subscribe((id) => selected.push(id));
    component.onSelect(stations[1]);
    expect(selected[selected.length - 1]).toBe('CHGR-201');
  });
});
