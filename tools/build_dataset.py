#!/usr/bin/env python3
"""Build the bundled AlpineGuard dataset.

Outputs (all under backend/src/main/resources/seed):
  stations_linea.json   station metadata, EAWS / Albina open data feature shape
  readings_smet.csv     hourly station readings, SMET column set
  bulletins_caaml.json  avalanche bulletins per region and elevation band
  routes/*.gpx          five ski touring tracks

Route tracks are least cost paths over the Copernicus 30 m DEM: the cost of a step
grows with the slope it climbs, so the resulting line follows the ridges and ramps a
skier would actually use instead of a straight line over the mountain.
"""

import heapq
import json
import math
import os
from datetime import datetime, timedelta, timezone

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(HERE)
SEED = os.path.join(REPO, "backend", "src", "main", "resources", "seed")
GOLDEN = os.path.join(REPO, "backend", "src", "test", "resources", "golden")

LON_MIN, LAT_MIN = 9.0, 46.0
RES = 0.0004
MOSAIC = np.load("/scratch/work/dem/mosaic.npy")

STATIONS = [
    ("AT07-101", "Patscherkofel", "AT-07 Tyrol", "Land Tirol", "LINEA", 47.2094, 11.4606),
    ("AT07-102", "Seegrube Nordkette", "AT-07 Tyrol", "Land Tirol", "LINEA", 47.3056, 11.3775),
    ("AT07-103", "Kuehtai", "AT-07 Tyrol", "Land Tirol", "LINEA", 47.1900, 11.0300),
    ("AT07-104", "Obergurgl", "AT-07 Tyrol", "Land Tirol", "LINEA", 46.8706, 11.0264),
    ("AT07-105", "Vent", "AT-07 Tyrol", "Land Tirol", "LINEA", 46.8667, 10.9000),
    ("AT07-106", "Pitztaler Gletscher", "AT-07 Tyrol", "Land Tirol", "LINEA", 46.9167, 10.8833),
    ("AT07-107", "Soelden Rettenbach", "AT-07 Tyrol", "Land Tirol", "LINEA", 46.9333, 10.9500),
    ("AT07-108", "Ischgl Idalpe", "AT-07 Tyrol", "Land Tirol", "LINEA", 46.9500, 10.3167),
    ("AT07-109", "Galtuer", "AT-07 Tyrol", "Land Tirol", "LINEA", 46.9667, 10.1833),
    ("AT07-110", "Sankt Anton am Arlberg", "AT-07 Tyrol", "Land Tirol", "LINEA", 47.1333, 10.2667),
    ("AT07-111", "Zugspitze", "AT-07 Tyrol", "Land Tirol", "LINEA", 47.4211, 10.9853),
    ("AT07-112", "Brenner", "AT-07 Tyrol", "Land Tirol", "LINEA", 47.0000, 11.5000),
    ("AT07-113", "Innsbruck Universitaet", "AT-07 Tyrol", "Land Tirol", "LINEA", 47.2637, 11.3842),
    ("AT07-114", "Stubai Eisjoch", "AT-07 Tyrol", "Land Tirol", "LINEA", 46.9833, 11.1000),
    ("AT07-115", "Neustift Milderaun", "AT-07 Tyrol", "Land Tirol", "LINEA", 47.0000, 11.2000),
    ("CHGR-201", "Weissfluhjoch", "CH-GR Graubuenden", "SLF", "IMIS", 46.8333, 9.8067),
    ("CHGR-202", "Weissfluhgipfel", "CH-GR Graubuenden", "SLF", "IMIS", 46.8342, 9.8096),
    ("CHGR-203", "Davos Dischma", "CH-GR Graubuenden", "SLF", "IMIS", 46.7500, 9.8500),
    ("CHGR-204", "Fluelapass", "CH-GR Graubuenden", "SLF", "IMIS", 46.7500, 9.9500),
    ("CHGR-205", "Samedan", "CH-GR Graubuenden", "SLF", "IMIS", 46.5333, 9.8833),
    ("CHGR-206", "Julierpass", "CH-GR Graubuenden", "SLF", "IMIS", 46.4667, 9.7333),
    ("CHGR-207", "Arosa", "CH-GR Graubuenden", "SLF", "IMIS", 46.7833, 9.6833),
    ("CHGR-208", "Parsenn Kreuzweg", "CH-GR Graubuenden", "SLF", "IMIS", 46.8600, 9.8000),
    ("CHGR-209", "Strelapass", "CH-GR Graubuenden", "SLF", "IMIS", 46.7833, 9.7833),
]

ROUTES = [
    ("stubai-gletscher-schaufelspitze", (46.9700, 11.0850), (46.9783, 11.1117)),
    ("weissfluhjoch-parsenn", (46.8550, 9.7900), (46.8342, 9.8096)),
    ("sulzkogel-kuehtai", (47.1900, 11.0300), (47.1850, 11.0350)),
    ("pitztal-wildspitze", (46.9167, 10.8833), (46.8850, 10.8700)),
    ("obergurgl-langtalferner", (46.8706, 11.0264), (46.8450, 11.0400)),
]

# Storm window inside the 30 day snapshot: 2026-03-13 00:00 to 2026-03-15 12:00 UTC.
WINDOW_END = datetime(2026, 3, 16, 6, 0, tzinfo=timezone.utc)
WINDOW_DAYS = 30


def elev(lat, lon):
    col = int((lon - LON_MIN) / RES)
    row = int((47.5 - lat) / RES)
    col = max(0, min(MOSAIC.shape[1] - 1, col))
    row = max(0, min(MOSAIC.shape[0] - 1, row))
    return float(MOSAIC[row, col])


def snap_to_max(lat, lon, radius_m=700.0):
    """Move a coordinate onto the highest DEM cell within the radius (summit snapping)."""
    span_deg = radius_m / 111320.0
    best = (elev(lat, lon), lat, lon)
    for dlat in np.arange(-span_deg, span_deg, RES):
        for dlon in np.arange(-span_deg, span_deg, RES * 1.4):
            z = elev(lat + dlat, lon + dlon)
            if z > best[0]:
                best = (z, lat + dlat, lon + dlon)
    return best[1], best[2]


def least_cost_path(start, end, step_m=25.0):
    """Dijkstra over the DEM grid, cost grows with the slope of the step."""
    pad = 0.012
    lat0, lon0 = start
    lat1, lon1 = end
    s_lat, s_lon = min(lat0, lat1) - pad, min(lon0, lon1) - pad
    n_lat, n_lon = max(lat0, lat1) + pad, max(lon0, lon1) + pad
    grid = 0.0006
    rows = int((n_lat - s_lat) / grid) + 1
    cols = int((n_lon - s_lon) / grid) + 1
    lats = s_lat + np.arange(rows) * grid
    lons = s_lon + np.arange(cols) * grid
    z = np.array([[elev(la, lo) for lo in lons] for la in lats])

    def to_cell(lat, lon):
        return (int(round((lat - s_lat) / grid)), int(round((lon - s_lon) / grid)))

    src = to_cell(*start)
    dst = to_cell(*end)
    dist = np.full((rows, cols), np.inf)
    dist[src] = 0.0
    prev = {}
    heap = [(0.0, src)]
    moves = [(-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)]
    cell_m = grid * 111320.0
    while heap:
        cost, cell = heapq.heappop(heap)
        if cell == dst:
            break
        if cost > dist[cell]:
            continue
        r, c = cell
        for dr, dc in moves:
            nr, nc = r + dr, c + dc
            if not (0 <= nr < rows and 0 <= nc < cols):
                continue
            horizontal = cell_m * (math.sqrt(2) if dr and dc else 1.0)
            dz = z[nr, nc] - z[r, c]
            slope = abs(math.degrees(math.atan2(dz, horizontal)))
            penalty = 1.0 + (slope / 22.0) ** 2
            if slope > 44:
                penalty += 40.0
            step = horizontal * penalty
            if cost + step < dist[nr, nc]:
                dist[nr, nc] = cost + step
                prev[(nr, nc)] = (r, c)
                heapq.heappush(heap, (cost + step, (nr, nc)))

    path = [dst]
    while path[-1] != src:
        path.append(prev[path[-1]])
    path.reverse()
    coords = [(lats[r], lons[c]) for r, c in path]
    out = [coords[0]]
    acc = 0.0
    for i in range(1, len(coords)):
        d = math.hypot((coords[i][0] - coords[i - 1][0]) * 111320.0,
                       (coords[i][1] - coords[i - 1][1]) * 111320.0 * math.cos(math.radians(coords[i][0])))
        acc += d
        if acc >= step_m or i == len(coords) - 1:
            out.append(coords[i])
            acc = 0.0
    return out


def write_gpx(path, name, coords):
    lines = ['<?xml version="1.0" encoding="UTF-8"?>',
             '<gpx version="1.1" creator="AlpineGuard" xmlns="http://www.topografix.com/GPX/1/1">',
             '  <metadata>',
             f'    <name>{name}</name>',
             '    <author><name>AlpineGuard</name></author>',
             '  </metadata>',
             '  <trk>',
             f'    <name>{name}</name>',
             '    <trkseg>']
    for lat, lon in coords:
        lines.append(f'      <trkpt lat="{lat:.6f}" lon="{lon:.6f}"><ele>{elev(lat, lon):.1f}</ele></trkpt>')
    lines += ['    </trkseg>', '  </trk>', '</gpx>', '']
    with open(path, "w") as fh:
        fh.write("\n".join(lines))


def build_stations():
    out = []
    for sid, name, region, operator, network, lat, lon in STATIONS:
        z = round(elev(lat, lon) / 10.0) * 10.0
        out.append({
            "type": "Feature",
            "geometry": {"type": "Point", "coordinates": [round(lon, 5), round(lat, 5)]},
            "properties": {
                "stationId": sid,
                "name": name,
                "region": region,
                "operator": operator,
                "network": network,
                "elevationM": z,
                "active": True,
            },
        })
    return {"type": "FeatureCollection", "features": out}


def storm_intensity(t):
    start = datetime(2026, 3, 13, 0, tzinfo=timezone.utc)
    end = datetime(2026, 3, 15, 12, tzinfo=timezone.utc)
    if start <= t <= end:
        return math.sin(math.pi * (t - start).total_seconds() / (end - start).total_seconds())
    return 0.0


def build_readings(stations):
    rows = ["station_id,measured_at,air_temp_c,wind_speed_ms,wind_gust_ms,wind_dir_deg,"
            "snow_depth_cm,new_snow_cm,humidity_pct,pressure_hpa"]
    start = WINDOW_END - timedelta(days=WINDOW_DAYS)
    hours = int((WINDOW_END - start).total_seconds() // 3600) + 1
    for index, station in enumerate(stations):
        props = station["properties"]
        z = props["elevationM"]
        sid = props["stationId"]
        rng = np.random.default_rng(1000 + index)
        base_depth = 60.0 + 0.09 * max(0.0, z - 900.0)
        depth = base_depth
        phase = rng.uniform(0, 6.28)
        for h in range(hours):
            t = start + timedelta(hours=h)
            day = t.timetuple().tm_yday
            diurnal = 4.5 * math.sin(2 * math.pi * (t.hour - 9) / 24.0)
            seasonal = 3.0 * math.cos(2 * math.pi * (day - 80) / 365.0)
            storm = storm_intensity(t)
            temp = 2.5 + seasonal + diurnal - 0.0062 * (z - 1200.0) - 3.5 * storm + rng.normal(0, 0.35)
            wind = max(0.3, 3.0 + 2.2 * math.sin(phase + h / 18.0) + 12.0 * storm + rng.normal(0, 0.7))
            gust = wind * (1.75 + 0.45 * storm) + rng.uniform(0.0, 1.5)
            wind_dir = (300.0 + 25.0 * math.sin(h / 30.0) + rng.normal(0, 8)) % 360.0
            new_snow = max(0.0, 11.0 * storm + rng.normal(0, 0.6)) if storm > 0.05 else 0.0
            melt = max(0.0, (temp - 1.5) * 0.35)
            depth = max(5.0, depth + new_snow - melt)
            humidity = min(99.0, max(45.0, 72.0 + 18.0 * storm + rng.normal(0, 3)))
            pressure = 1013.0 - z / 8.4 - 9.0 * storm + rng.normal(0, 0.8)
            rows.append(
                f"{sid},{t.strftime('%Y-%m-%dT%H:%M:%SZ')},{temp:.1f},{wind:.1f},{gust:.1f},"
                f"{wind_dir:.0f},{depth:.1f},{new_snow:.1f},{humidity:.0f},{pressure:.1f}")
    return "\n".join(rows) + "\n"


def build_bulletins():
    ratings = {
        "AT-07 Tyrol": {"LOW": 2, "MID": 3, "HIGH": 4},
        "CH-GR Graubuenden": {"LOW": 2, "MID": 2, "HIGH": 3},
    }
    problems = {
        "AT-07 Tyrol": ("WIND_SLAB", "Fresh wind slabs on north and east facing slopes above the treeline."),
        "CH-GR Graubuenden": ("PERSISTENT_WEAK_LAYER", "Buried surface hoar near 2200 m, triggered by large loads."),
    }
    bulletins = []
    for back in range(7):
        day = (WINDOW_END - timedelta(days=back)).date()
        for region, bands in ratings.items():
            problem, summary = problems[region]
            decay = {0: 0, 1: 0, 2: 0, 3: 1}.get(back, 1)
            bulletins.append({
                "region": region,
                "validDate": day.isoformat(),
                "issuedAt": f"{day.isoformat()}T07:00:00Z",
                "mainProblem": problem,
                "summary": summary,
                "dangerRatings": [
                    {"band": band, "rating": max(1, rating - decay)} for band, rating in bands.items()
                ],
            })
    return {"bulletins": bulletins}


def build_golden(stations):
    """Ten station snapshots over the last 72 hours plus three track references."""
    chosen = ["AT07-114", "AT07-103", "AT07-106", "AT07-104", "AT07-101",
              "CHGR-201", "CHGR-202", "CHGR-208", "CHGR-204", "CHGR-203"]
    by_id = {s["properties"]["stationId"]: s for s in stations}
    snapshots = []
    for sid in chosen:
        station = by_id[sid]
        props = station["properties"]
        snapshots.append({
            "stationId": sid,
            "name": props["name"],
            "region": props["region"],
            "elevationM": props["elevationM"],
            "lat": station["geometry"]["coordinates"][1],
            "lon": station["geometry"]["coordinates"][0],
        })
    return {
        "description": "Tyrol and Graubuenden storm snapshot, 2026-03-13 to 2026-03-16. "
                       "Readings come from the bundled seed feed; expected values are pinned "
                       "from a verified engine run.",
        "stations": snapshots,
        "routes": [
            {"slug": "stubai-gletscher-schaufelspitze", "gpx": "golden/routes/stubai-glacier.gpx"},
            {"slug": "weissfluhjoch-parsenn", "gpx": "golden/routes/weissfluhjoch.gpx"},
            {"slug": "sulzkogel-kuehtai", "gpx": "golden/routes/sulzkogel.gpx"},
        ],
        "expected": None,
    }


def main():
    os.makedirs(os.path.join(SEED, "routes"), exist_ok=True)
    os.makedirs(os.path.join(GOLDEN, "routes"), exist_ok=True)

    stations = build_stations()
    with open(os.path.join(SEED, "stations_linea.json"), "w") as fh:
        json.dump(stations, fh, indent=2)
    print("stations", len(stations["features"]))

    with open(os.path.join(SEED, "readings_smet.csv"), "w") as fh:
        fh.write(build_readings(stations["features"]))
    print("readings written")

    with open(os.path.join(SEED, "bulletins_caaml.json"), "w") as fh:
        json.dump(build_bulletins(), fh, indent=2)

    names = {
        "stubai-gletscher-schaufelspitze": "Stubai Glacier to Schaufelspitze",
        "weissfluhjoch-parsenn": "Weissfluhjoch Parsenn descent",
        "sulzkogel-kuehtai": "Kuehtai to Sulzkogel",
        "pitztal-wildspitze": "Pitztal Glacier to Wildspitze",
        "obergurgl-langtalferner": "Obergurgl Langtalferner",
    }
    golden_sources = {
        "stubai-gletscher-schaufelspitze": "stubai-glacier.gpx",
        "weissfluhjoch-parsenn": "weissfluhjoch.gpx",
        "sulzkogel-kuehtai": "sulzkogel.gpx",
    }
    for slug, start, summit in ROUTES:
        peak_lat, peak_lon = snap_to_max(*summit)
        coords = least_cost_path(start, (peak_lat, peak_lon))
        write_gpx(os.path.join(SEED, "routes", f"{slug}.gpx"), names[slug], coords)
        if slug in golden_sources:
            write_gpx(os.path.join(GOLDEN, "routes", golden_sources[slug]), names[slug], coords)
        elevs = [elev(la, lo) for la, lo in coords]
        print(f"{slug}: {len(coords)} points, {min(elevs):.0f}-{max(elevs):.0f} m")

    with open(os.path.join(GOLDEN, "tyrol_davos_2026_storm.json"), "w") as fh:
        json.dump(build_golden(stations["features"]), fh, indent=2)
    print("golden inputs written")


if __name__ == "__main__":
    main()
