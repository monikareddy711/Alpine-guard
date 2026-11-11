import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { HazardAssessment, RouteDetail, SegmentHazard } from '../core/models';

@Component({
  selector: 'ag-hazard-panel',
  standalone: true,
  imports: [CommonModule, TableModule, TagModule],
  template: `
    <section class="ag-panel hazard-panel">
      <div class="ag-panel-title">
        <span>Route hazard breakdown</span>
        <span class="hint">{{ assessment?.totalSegments ?? 0 }} segments · {{ windowHours }} h window</span>
      </div>

      <div class="summary" *ngIf="route">
        <div class="summary-head">
          <span class="route-name">{{ route.name }}</span>
          <p-tag
            [value]="assessment?.severity ?? 'n/a'"
            [style]="{ background: 'transparent', border: '1px solid currentColor' }"
            [ngClass]="severityClass()"
          ></p-tag>
        </div>
        <div class="metrics">
          <div><span class="label">distance</span><span class="value">{{ (route.distanceM / 1000).toFixed(1) }} km</span></div>
          <div><span class="label">ascent</span><span class="value">{{ route.ascentM | number: '1.0-0' }} m</span></div>
          <div><span class="label">max slope</span><span class="value">{{ assessment?.maxSlopeDeg ?? 0 }}°</span></div>
          <div><span class="label">steep segs</span><span class="value">{{ assessment?.steepSegmentCount ?? 0 }}</span></div>
          <div><span class="label">peak score</span><span class="value">{{ assessment?.maxScore ?? 0 }}</span></div>
          <div><span class="label">grade</span><span class="value">{{ route.difficulty }}</span></div>
        </div>
      </div>

      <p-table
        [value]="assessment?.segments ?? []"
        [paginator]="true"
        [rows]="7"
        [scrollable]="true"
        scrollHeight="flex"
        selectionMode="single"
        [(selection)]="selectedSegment"
        (onRowSelect)="onSelect($event.data)"
        styleClass="segment-grid"
      >
        <ng-template pTemplate="header">
          <tr>
            <th>#</th>
            <th>elev</th>
            <th>slope</th>
            <th>score</th>
            <th>severity</th>
            <th>station</th>
            <th>flags</th>
          </tr>
        </ng-template>
        <ng-template pTemplate="body" let-segment>
          <tr [pSelectableRow]="segment">
            <td>{{ segment.index }}</td>
            <td>{{ segment.elevationM | number: '1.0-0' }} m</td>
            <td [class.steep]="segment.slopeDeg >= 30">{{ segment.slopeDeg }}°</td>
            <td>{{ segment.score }}</td>
            <td>
              <p-tag
                [value]="segment.severity"
                [style]="{ background: 'transparent', border: '1px solid currentColor' }"
                [ngClass]="'ag-severity-' + segment.severity.toLowerCase()"
              ></p-tag>
            </td>
            <td class="station">
              {{ segment.nearestStationName }}<span class="dist">{{ (segment.nearestStationDistanceM / 1000).toFixed(1) }} km</span>
            </td>
            <td class="flags">{{ segment.flags.join(', ') || '–' }}</td>
          </tr>
        </ng-template>
      </p-table>
    </section>
  `,
  styles: [
    `
      .hazard-panel {
        display: flex;
        flex-direction: column;
        height: 100%;
      }
      .summary {
        padding: 8px 12px;
        border-bottom: 1px solid var(--ag-border);
      }
      .summary-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        margin-bottom: 6px;
      }
      .route-name {
        font-weight: 600;
      }
      .metrics {
        display: grid;
        grid-template-columns: repeat(6, minmax(0, 1fr));
        gap: 4px 8px;
      }
      .metrics div {
        display: flex;
        flex-direction: column;
      }
      .label {
        color: var(--ag-muted);
        font-size: 10px;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }
      .value {
        font-size: 12px;
      }
      .steep {
        color: var(--ag-high);
        font-weight: 600;
      }
      .station {
        font-size: 11px;
      }
      .dist {
        color: var(--ag-muted);
        margin-left: 4px;
      }
      .flags {
        font-size: 10px;
        color: var(--ag-muted);
      }
      .hint {
        text-transform: none;
        letter-spacing: 0;
        font-size: 11px;
      }
      :host ::ng-deep .segment-grid table {
        font-size: 11px;
      }
      :host ::ng-deep .segment-grid .p-datatable-thead > tr > th {
        background: #1b2430;
        color: var(--ag-muted);
        font-size: 10px;
        text-transform: uppercase;
      }
      :host ::ng-deep .segment-grid .p-tag {
        font-size: 10px;
      }
    `
  ]
})
export class HazardPanelComponent {
  @Input() route: RouteDetail | null = null;
  @Input() assessment: HazardAssessment | null = null;
  @Input() windowHours = 72;
  @Output() segmentSelected = new EventEmitter<SegmentHazard>();

  selectedSegment: SegmentHazard | null = null;

  onSelect(segment: SegmentHazard): void {
    this.segmentSelected.emit(segment);
  }

  severityClass(): string {
    const severity = this.assessment?.severity ?? 'LOW';
    return `ag-severity-${severity.toLowerCase()}`;
  }
}
