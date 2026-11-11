import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { DashboardStore } from '../core/dashboard-store.service';
import { ElevationBand, Station, StationFilter } from '../core/models';
import { DANGER_LABELS } from '../core/severity';
import { DEFAULT_FILTER, filterStations, regionsOf } from '../core/station-filter';

@Component({
  selector: 'ag-station-table',
  standalone: true,
  imports: [CommonModule, FormsModule, TableModule, DropdownModule, SelectButtonModule, TagModule, InputTextModule],
  template: `
    <section class="ag-panel table-panel">
      <div class="ag-panel-title">
        <span>Weather stations ({{ visible.length }} / {{ all.length }})</span>
        <span class="hint">click a row to move the terrain camera</span>
      </div>

      <div class="filters">
        <input
          pInputText
          type="text"
          placeholder="search station"
          [(ngModel)]="filter.text"
          (ngModelChange)="apply()"
          class="search"
        />
        <p-dropdown
          [options]="regions"
          [(ngModel)]="filter.region"
          (ngModelChange)="apply()"
          placeholder="all regions"
          [showClear]="true"
          styleClass="filter-drop"
        ></p-dropdown>
        <p-selectButton
          [options]="bandOptions"
          [(ngModel)]="filter.band"
          (ngModelChange)="apply()"
          optionLabel="label"
          optionValue="value"
          [allowEmpty]="true"
        ></p-selectButton>
        <p-dropdown
          [options]="dangerOptions"
          [(ngModel)]="filter.minDanger"
          (ngModelChange)="apply()"
          placeholder="any danger"
          [showClear]="true"
          styleClass="filter-drop"
        ></p-dropdown>
      </div>

      <p-table
        [value]="visible"
        [paginator]="true"
        [rows]="9"
        [scrollable]="true"
        scrollHeight="flex"
        selectionMode="single"
        [(selection)]="selectedRow"
        (onRowSelect)="onSelect($event.data)"
        styleClass="station-grid"
      >
        <ng-template pTemplate="header">
          <tr>
            <th pSortableColumn="name">station <p-sortIcon field="name"></p-sortIcon></th>
            <th pSortableColumn="elevationM">elev <p-sortIcon field="elevationM"></p-sortIcon></th>
            <th pSortableColumn="dangerRating">danger <p-sortIcon field="dangerRating"></p-sortIcon></th>
            <th>snow</th>
            <th>gust</th>
            <th>new 24h</th>
          </tr>
        </ng-template>
        <ng-template pTemplate="body" let-station>
          <tr [pSelectableRow]="station">
            <td>
              <div class="name">{{ station.name }}</div>
              <div class="sub">{{ station.stationId }} · {{ station.region }}</div>
            </td>
            <td>{{ station.elevationM | number: '1.0-0' }} m</td>
            <td>
              <p-tag
                [value]="dangerLabel(station.dangerRating)"
                [style]="{ background: 'transparent', border: '1px solid currentColor' }"
                [ngClass]="'ag-severity-' + station.severity.toLowerCase()"
              ></p-tag>
            </td>
            <td>{{ station.latestReading?.snowDepthCm ?? '–' }} cm</td>
            <td>{{ station.latestReading?.windGustMs ?? '–' }} m/s</td>
            <td>{{ newSnow(station) }} cm</td>
          </tr>
        </ng-template>
      </p-table>
    </section>
  `,
  styles: [
    `
      .table-panel {
        display: flex;
        flex-direction: column;
        height: 100%;
      }
      .filters {
        display: flex;
        gap: 6px;
        padding: 8px 10px;
        flex-wrap: wrap;
        align-items: center;
      }
      .search {
        width: 130px;
      }
      .filter-drop {
        min-width: 118px;
      }
      .name {
        font-weight: 600;
      }
      .sub {
        color: var(--ag-muted);
        font-size: 11px;
      }
      .hint {
        text-transform: none;
        letter-spacing: 0;
        font-size: 11px;
      }
      :host ::ng-deep .station-grid .p-datatable-wrapper {
        flex: 1;
      }
      :host ::ng-deep .station-grid table {
        font-size: 12px;
      }
      :host ::ng-deep .station-grid .p-datatable-thead > tr > th {
        background: #1b2430;
        color: var(--ag-muted);
        font-size: 11px;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }
      :host ::ng-deep .p-tag {
        font-size: 11px;
      }
    `
  ]
})
export class StationTableComponent {
  private readonly store = inject(DashboardStore);

  @Input() set stations(value: Station[]) {
    this.all = value ?? [];
    this.regions = regionsOf(this.all);
    this.apply();
  }
  get stations(): Station[] {
    return this.all;
  }

  all: Station[] = [];
  visible: Station[] = [];
  regions: string[] = [];
  filter: StationFilter = { ...DEFAULT_FILTER };
  selectedRow: Station | null = null;

  bandOptions = [
    { label: 'all', value: null },
    { label: '> 2000 m', value: 'MID' as ElevationBand },
    { label: '> 2800 m', value: 'HIGH' as ElevationBand }
  ];

  dangerOptions = [
    { label: '2+ moderate', value: 2 },
    { label: '3+ considerable', value: 3 },
    { label: '4+ high', value: 4 }
  ];

  apply(): void {
    this.visible = filterStations(this.all, this.filter);
  }

  onSelect(station: Station): void {
    this.store.selectStation(station.stationId);
  }

  dangerLabel(rating: number | null): string {
    return rating ? `${rating} ${DANGER_LABELS[rating] ?? ''}`.trim() : 'n/a';
  }

  newSnow(station: Station): string {
    return station.latestReading?.newSnowCm != null ? `${station.latestReading.newSnowCm}` : '–';
  }
}
