import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnDestroy,
  ViewChild,
  inject
} from '@angular/core';
import * as Cesium from 'cesium';
import { Subscription } from 'rxjs';
import { DashboardStore, FocusTarget } from '../core/dashboard-store.service';
import { HazardAssessment, RouteDetail, Station } from '../core/models';
import { cameraDestination, stationMarkerPosition, stationPositionOf } from '../core/cesium-geo';
import { severityColor, slopeColor } from '../core/severity';
import { TerrainManifest, createHeightmapTerrainProvider, imageryProviderFor } from './heightmap-terrain-provider';

@Component({
  selector: 'ag-terrain-globe',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="ag-panel globe-panel">
      <div class="ag-panel-title">
        <span>Alpine terrain · {{ status }}</span>
        <span class="hint">digital elevation model with route and station overlays</span>
      </div>
      <div #container class="globe"></div>
    </section>
  `,
  styles: [
    `
      .globe-panel {
        display: flex;
        flex-direction: column;
        height: 100%;
      }
      .globe {
        flex: 1;
        min-height: 0;
        position: relative;
      }
      .hint {
        text-transform: none;
        letter-spacing: 0;
        font-size: 11px;
      }
    `
  ]
})
export class TerrainGlobeComponent implements AfterViewInit, OnDestroy {
  @ViewChild('container', { static: true }) container!: ElementRef<HTMLDivElement>;

  @Input() set stations(value: Station[]) {
    this.stationData = value ?? [];
    this.renderStations();
  }
  @Input() set route(value: RouteDetail | null) {
    this.routeData = value;
    this.renderRoute();
  }
  @Input() set assessment(value: HazardAssessment | null) {
    this.assessmentData = value;
    this.renderRoute();
  }

  private readonly store = inject(DashboardStore);
  private readonly zone = inject(NgZone);

  viewer: Cesium.Viewer | null = null;
  status = 'loading';
  private stationData: Station[] = [];
  private routeData: RouteDetail | null = null;
  private assessmentData: HazardAssessment | null = null;
  private routeEntities: Cesium.Entity[] = [];
  private stationEntities: Cesium.Entity[] = [];
  private subscription = new Subscription();
  private framedSlug: string | null = null;
  private ready = false;

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => {
      void this.initViewer();
    });
    this.subscription.add(this.store.focus$.subscribe((target) => this.focus(target)));
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    this.viewer?.destroy();
  }

  private async initViewer(): Promise<void> {
    const manifest: TerrainManifest = await fetch('terrain/manifest.json').then((response) => response.json());
    const viewer = new Cesium.Viewer(this.container.nativeElement, {
      terrainProvider: createHeightmapTerrainProvider(manifest),
      baseLayer: new Cesium.ImageryLayer(imageryProviderFor(manifest)),
      baseLayerPicker: false,
      geocoder: false,
      homeButton: false,
      sceneModePicker: false,
      navigationHelpButton: false,
      animation: false,
      timeline: false,
      fullscreenButton: false,
      infoBox: false,
      selectionIndicator: false,
      shouldAnimate: false,
      creditContainer: document.createElement('div')
    });
    viewer.scene.globe.depthTestAgainstTerrain = true;
    viewer.scene.globe.showGroundAtmosphere = true;
    viewer.scene.skyAtmosphere.show = true;
    viewer.scene.fog.enabled = true;
    viewer.scene.screenSpaceCameraController.maximumZoomDistance = 240000;

    viewer.camera.setView({
      destination: Cesium.Cartesian3.fromDegrees(11.05, 46.98, 22000),
      orientation: { heading: 0.0, pitch: Cesium.Math.toRadians(-48), roll: 0.0 }
    });

    this.viewer = viewer;
    // Exposed so the end to end specs can assert on camera state and scene content.
    (window as unknown as { alpineGuardViewer?: Cesium.Viewer }).alpineGuardViewer = viewer;
    this.ready = true;
    this.status = 'ready';
    this.renderStations();
    this.renderRoute();
  }

  private focus(target: FocusTarget): void {
    const viewer = this.viewer;
    if (!viewer) {
      return;
    }
    this.zone.runOutsideAngular(() => {
      viewer.camera.flyTo({
        destination: Cesium.Cartesian3.fromElements(...cameraDestination(target.lat, target.lon, target.height)),
        orientation: { heading: 0.0, pitch: Cesium.Math.toRadians(-38), roll: 0.0 },
        duration: 0.8
      });
    });
    this.status = `focused: ${target.label}`;
  }

  private renderStations(): void {
    const viewer = this.viewer;
    if (!viewer || !this.ready) {
      return;
    }
    for (const entity of this.stationEntities) {
      viewer.entities.remove(entity);
    }
    this.stationEntities = this.stationData.map((station) => {
      const color = Cesium.Color.fromCssColorString(severityColor(station.severity));
      return viewer.entities.add({
        id: `station-${station.stationId}`,
        position: Cesium.Cartesian3.fromElements(...stationMarkerPosition(station)),
        point: {
          pixelSize: 9,
          color,
          outlineColor: Cesium.Color.BLACK.withAlpha(0.65),
          outlineWidth: 1.5,
          disableDepthTestDistance: Number.POSITIVE_INFINITY
        },
        label: {
          text: station.name,
          font: '11px Inter, sans-serif',
          fillColor: Cesium.Color.fromCssColorString('#d7dee7'),
          showBackground: true,
          backgroundColor: Cesium.Color.fromCssColorString('#10151c').withAlpha(0.65),
          pixelOffset: new Cesium.Cartesian2(0, -16),
          disableDepthTestDistance: Number.POSITIVE_INFINITY,
          scale: 0.9
        }
      });
    });
  }

  private renderRoute(): void {
    const viewer = this.viewer;
    if (!viewer || !this.ready || !this.routeData) {
      return;
    }
    for (const entity of this.routeEntities) {
      viewer.entities.remove(entity);
    }
    const segments = this.assessmentData?.segments ?? [];
    const trackPositions = this.routeData.points.map((point) =>
      Cesium.Cartesian3.fromElements(...stationPositionOf(point.lat, point.lon, point.elevationM))
    );
    this.routeEntities = segments.map((segment) => {
      const color = Cesium.Color.fromCssColorString(slopeColor(segment.slopeDeg));
      return viewer.entities.add({
        polyline: {
          positions: Cesium.Cartesian3.fromDegreesArray([
            segment.startLon,
            segment.startLat,
            segment.endLon,
            segment.endLat
          ]),
          width: 5,
          material: new Cesium.PolylineOutlineMaterialProperty({
            color,
            outlineColor: Cesium.Color.BLACK.withAlpha(0.45),
            outlineWidth: 1
          }),
          clampToGround: true,
          arcType: Cesium.ArcType.GEODESIC
        }
      });
    });

    if (this.framedSlug !== this.routeData.slug && trackPositions.length > 1) {
      this.framedSlug = this.routeData.slug;
      const sphere = Cesium.BoundingSphere.fromPoints(trackPositions);
      viewer.camera.flyToBoundingSphere(sphere, {
        offset: new Cesium.HeadingPitchRange(
          0,
          Cesium.Math.toRadians(-42),
          Math.max(6000, sphere.radius * 3.4)
        ),
        duration: 1.4
      });
      this.status = `route: ${this.routeData.name}`;
    }
  }
}
