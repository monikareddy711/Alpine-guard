#!/usr/bin/env python3
"""Build Cesium heightmap terrain tiles and hillshade imagery tiles.

Input: Copernicus DEM 30 m COG tiles (AWS open data, EPSG:4326).
Output: frontend/public/terrain/{heightmap,imagery}/... plus manifest.json.

Heightmap tiles are raw little endian uint16 buffers, 65x65 samples, holding
(elevation + 50) / 0.1. Sample (0,0) is the north-west corner of the tile
rectangle; Cesium maps an n*n buffer onto the tile extent so that sample
(n-1,n-1) is the south-east corner, therefore 65 samples cover 64 intervals.
"""

import json
import os
import sys

import numpy as np
import rasterio
from PIL import Image
from rasterio.enums import Resampling
from rasterio.transform import Affine
from rasterio.warp import reproject

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
DEM_DIR = os.environ.get("DEM_DIR", "/scratch/work/dem")
OUT = os.path.join(REPO, "frontend", "public", "terrain")

# Coverage: western Graubünden (Davos) through the Stubai and Ötztal Alps.
LON_MIN, LON_MAX = 9.0, 12.0
LAT_MIN, LAT_MAX = 46.0, 47.5
MOSAIC_RES = 0.0004  # about 31 m at 46.7 N

TILES_X = 8
TILES_Y = 4
LEVELS = 3           # level 0: 8x4, level 1: 16x8, level 2: 32x16
HM_SAMPLES = 65
IMAGERY_PX = 512


def build_mosaic():
    cache = os.path.join(DEM_DIR, "mosaic.npy")
    if os.path.exists(cache):
        return np.load(cache)
    w = int(round((LON_MAX - LON_MIN) / MOSAIC_RES))
    h = int(round((LAT_MAX - LAT_MIN) / MOSAIC_RES))
    dst_transform = Affine(MOSAIC_RES, 0.0, LON_MIN, 0.0, -MOSAIC_RES, LAT_MAX)
    mosaic = np.full((h, w), np.nan, dtype=np.float32)
    tiles = sorted(f for f in os.listdir(DEM_DIR) if f.endswith(".tif"))
    for name in tiles:
        with rasterio.open(os.path.join(DEM_DIR, name)) as src:
            band = src.read(1).astype(np.float32)
            scratch = np.full((h, w), np.nan, dtype=np.float32)
            reproject(
                source=band,
                destination=scratch,
                src_transform=src.transform,
                src_crs=src.crs,
                src_nodata=src.nodata,
                dst_transform=dst_transform,
                dst_crs=src.crs,
                dst_nodata=np.nan,
                resampling=Resampling.bilinear,
            )
            mosaic = np.where(np.isnan(scratch), mosaic, scratch)
    gaps = int(np.isnan(mosaic).sum())
    if gaps:
        print("filling", gaps, "nodata samples with the mean elevation")
        mosaic = np.nan_to_num(mosaic, nan=float(np.nanmean(mosaic)))
    np.save(cache, mosaic)
    return mosaic


def sample(mosaic, lons, lats):
    h, w = mosaic.shape
    cols = (lons - LON_MIN) / MOSAIC_RES
    rows = (LAT_MAX - lats) / MOSAIC_RES
    c0 = np.clip(np.floor(cols).astype(int), 0, w - 2)
    r0 = np.clip(np.floor(rows).astype(int), 0, h - 2)
    fx = np.clip(cols - c0, 0, 1)
    fy = np.clip(rows - r0, 0, 1)
    a = mosaic[r0, c0]
    b = mosaic[r0, c0 + 1]
    c = mosaic[r0 + 1, c0]
    d = mosaic[r0 + 1, c0 + 1]
    return (a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + d * fx * fy)


def hillshade(z, res_deg, lat):
    dy = res_deg * 111320.0
    dx = dy * np.cos(np.radians(lat))
    gy, gx = np.gradient(z, dy, dx)
    slope = np.arctan(np.hypot(gx, gy))
    aspect = np.arctan2(-gx, gy)
    az = np.radians(315.0)
    alt = np.radians(42.0)
    shaded = np.sin(alt) * np.cos(slope) + np.cos(alt) * np.sin(slope) * np.cos(az - aspect)
    return np.clip(shaded, 0, 1), np.degrees(slope)


def ramp(z):
    """Elevation tint: valley forest, alpine grass, rock, snow."""
    stops = [
        (300, (58, 74, 48)),
        (900, (78, 96, 56)),
        (1400, (120, 122, 74)),
        (1900, (150, 140, 104)),
        (2400, (140, 132, 126)),
        (2900, (176, 172, 168)),
        (3300, (232, 234, 236)),
        (4000, (250, 251, 252)),
    ]
    out = np.zeros(z.shape + (3,), dtype=np.float32)
    zs = np.array([s[0] for s in stops], dtype=np.float32)
    cs = np.array([s[1] for s in stops], dtype=np.float32)
    for ch in range(3):
        out[..., ch] = np.interp(z, zs, cs[:, ch])
    return out


def main():
    os.makedirs(os.path.join(OUT, "heightmap"), exist_ok=True)
    os.makedirs(os.path.join(OUT, "imagery", "0"), exist_ok=True)
    mosaic = build_mosaic()
    print("mosaic", mosaic.shape, float(np.nanmin(mosaic)), float(np.nanmax(mosaic)))

    manifest = {
        "rectangle": {"west": LON_MIN, "south": LAT_MIN, "east": LON_MAX, "north": LAT_MAX},
        "tilesX": TILES_X,
        "tilesY": TILES_Y,
        "levels": LEVELS,
        "heightmapSamples": HM_SAMPLES,
        "imageryPixels": IMAGERY_PX,
        "imageryMaxLevel": 0,
        "source": "Copernicus DEM GLO-30 (AWS Open Data, copernicus-dem-30m)",
    }

    tile_lon = (LON_MAX - LON_MIN) / TILES_X
    tile_lat = (LAT_MAX - LAT_MIN) / TILES_Y

    total = 0
    for level in range(LEVELS):
        nx, ny = TILES_X * (2 ** level), TILES_Y * (2 ** level)
        os.makedirs(os.path.join(OUT, "heightmap", str(level)), exist_ok=True)
        dlon = (LON_MAX - LON_MIN) / nx
        dlat = (LAT_MAX - LAT_MIN) / ny
        for y in range(ny):
            for x in range(nx):
                lons = LON_MIN + (x + np.arange(HM_SAMPLES) / (HM_SAMPLES - 1)) * dlon
                lats = LAT_MAX - (y + np.arange(HM_SAMPLES) / (HM_SAMPLES - 1)) * dlat
                grid_lon, grid_lat = np.meshgrid(lons, lats)
                z = sample(mosaic, grid_lon, grid_lat).astype(np.float32)
                z = np.clip(z, -50, 9000)
                # 0.1 m quantum with a -50 m offset keeps the whole alpine range in uint16.
                enc = np.clip((z + 50.0) / 0.1, 0, 65535).astype("<u2")
                enc.tofile(os.path.join(OUT, "heightmap", str(level), f"{x}_{y}.bin"))
                total += 1
        print("level", level, "tiles", nx * ny)

    nx, ny = TILES_X, TILES_Y
    for y in range(ny):
        for x in range(nx):
            lons = LON_MIN + (x + np.arange(IMAGERY_PX) / (IMAGERY_PX - 1)) * tile_lon
            lats = LAT_MAX - (y + np.arange(IMAGERY_PX) / (IMAGERY_PX - 1)) * tile_lat
            grid_lon, grid_lat = np.meshgrid(lons, lats)
            z = sample(mosaic, grid_lon, grid_lat).astype(np.float32)
            shade, slope = hillshade(z, tile_lon / IMAGERY_PX, float(grid_lat.mean()))
            base = ramp(z)
            light = 0.35 + 0.85 * shade
            rgb = np.clip(base * light[..., None], 0, 255)
            # Steep faces get a faint rock tint so couloirs read at a glance.
            steep = np.clip((slope - 32.0) / 20.0, 0, 1)
            rock = np.array([126, 118, 112], dtype=np.float32)
            rgb = rgb * (1 - 0.35 * steep[..., None]) + rock * (0.35 * steep[..., None])
            Image.fromarray(rgb.astype(np.uint8), mode="RGB").save(
                os.path.join(OUT, "imagery", "0", f"{x}_{y}.png"), optimize=True
            )
    print("heightmap tiles", total, "imagery tiles", nx * ny)

    with open(os.path.join(OUT, "manifest.json"), "w") as fh:
        json.dump(manifest, fh, indent=2)
    return 0


if __name__ == "__main__":
    sys.exit(main())
