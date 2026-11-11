import * as Cesium from 'cesium';

export interface TerrainManifest {
  rectangle: { west: number; south: number; east: number; north: number };
  tilesX: number;
  tilesY: number;
  levels: number;
  heightmapSamples: number;
  imageryPixels: number;
  imageryMaxLevel: number;
  source: string;
}

const HEIGHT_SCALE_M = 0.1;
const HEIGHT_OFFSET_M = -50;

/**
 * Serves the bundled Copernicus 30 m derived heightmaps as a Cesium terrain provider.
 * Each tile is a 65x65 little endian uint16 buffer; the first sample is the north west
 * corner of the tile rectangle, which is the layout HeightmapTerrainData expects.
 *
 * Cesium declares TerrainProvider as an interface rather than a base class that can be
 * instantiated, so the provider is a plain object implementing that interface.
 */
export function createHeightmapTerrainProvider(
  manifest: TerrainManifest,
  basePath = 'terrain/heightmap'
): Cesium.TerrainProvider {
  const rectangle = Cesium.Rectangle.fromDegrees(
    manifest.rectangle.west,
    manifest.rectangle.south,
    manifest.rectangle.east,
    manifest.rectangle.north
  );
  const scheme = new Cesium.GeographicTilingScheme({
    rectangle,
    numberOfLevelZeroTilesX: manifest.tilesX,
    numberOfLevelZeroTilesY: manifest.tilesY
  });
  const samples = manifest.heightmapSamples;
  const levels = manifest.levels;
  const levelZeroError = Cesium.TerrainProvider.getEstimatedLevelZeroGeometricErrorForAHeightmap(
    Cesium.Ellipsoid.WGS84,
    manifest.heightmapSamples,
    manifest.tilesX
  );

  const provider = {
    tilingScheme: scheme,
    hasWaterMask: false,
    hasVertexNormals: false,
    availability: undefined,
    credit: undefined,
    errorEvent: new Cesium.Event(),
    getLevelMaximumGeometricError(level: number): number {
      return levelZeroError / (1 << level);
    },
    getTileDataAvailable(x: number, y: number, level: number): boolean {
      const tilesX = manifest.tilesX * 2 ** level;
      const tilesY = manifest.tilesY * 2 ** level;
      return level < levels && x >= 0 && y >= 0 && x < tilesX && y < tilesY;
    },
    requestTileGeometry(x: number, y: number, level: number): Promise<Cesium.TerrainData> | undefined {
      if (level >= levels) {
        return undefined;
      }
      const url = `${basePath}/${level}/${x}_${y}.bin`;
      return fetch(url).then(async (response) => {
        if (!response.ok) {
          throw new Error(`terrain tile ${url} returned ${response.status}`);
        }
        const buffer = await response.arrayBuffer();
        const encoded = new Uint16Array(buffer);
        const heights = new Float32Array(encoded.length);
        for (let i = 0; i < encoded.length; i++) {
          heights[i] = encoded[i] * HEIGHT_SCALE_M + HEIGHT_OFFSET_M;
        }
        return new Cesium.HeightmapTerrainData({
          buffer: heights,
          width: samples,
          height: samples,
          childTileMask: level < levels - 1 ? 15 : 0
        });
      });
    },
    requestWaterMask: () => Promise.resolve(undefined),
    requestVertexNormals: () => Promise.resolve(undefined)
  };
  return provider as unknown as Cesium.TerrainProvider;
}

export function imageryProviderFor(manifest: TerrainManifest): Cesium.ImageryProvider {
  const rectangle = Cesium.Rectangle.fromDegrees(
    manifest.rectangle.west,
    manifest.rectangle.south,
    manifest.rectangle.east,
    manifest.rectangle.north
  );
  return new Cesium.UrlTemplateImageryProvider({
    url: 'terrain/imagery/{z}/{x}_{y}.png',
    tilingScheme: new Cesium.GeographicTilingScheme({
      rectangle,
      numberOfLevelZeroTilesX: manifest.tilesX,
      numberOfLevelZeroTilesY: manifest.tilesY
    }),
    tileWidth: manifest.imageryPixels,
    tileHeight: manifest.imageryPixels,
    minimumLevel: 0,
    maximumLevel: manifest.imageryMaxLevel,
    hasAlphaChannel: false
  });
}
