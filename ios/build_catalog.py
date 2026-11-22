#!/usr/bin/env python3
"""Build the read-only iOS catalogue from the existing Android data."""
import gzip
import hashlib
import json
import math
import sqlite3
import struct
import sys
import tempfile
import unicodedata
from io import BytesIO
from pathlib import Path

ASSETS = Path(__file__).resolve().parents[1] / "android-legacy" / "assets"


def normalize(value):
    decomposed = unicodedata.normalize("NFD", value.lower().replace("ı", "i"))
    return "".join(c for c in decomposed if not unicodedata.combining(c))


def stable_id(city, code):
    digest = hashlib.sha256(f"{city}:{code}".encode()).digest()
    return (1 << 53) + int.from_bytes(digest[:6], "big")


def coordinates(lat, lon):
    try:
        a, b = float(lat), float(lon)
    except (ValueError, TypeError):
        return None, None
    if not math.isfinite(a) or not math.isfinite(b) or not (0 < abs(a) < 85 and 0 < abs(b) < 180):
        return None, None
    return a, b


def add_regional_catalogues(db, cities):
    for city in cities:
        city_id = city["id"]
        path = ASSETS / f"city-catalog-{city_id}.dbz"
        if not path.is_file():
            continue
        with gzip.open(path, "rt", encoding="utf-8") as source:
            catalog = json.load(source)
        for row in catalog.get("routes", []):
            key = str(row["routeCode"])
            code = str(row.get("displayRouteCode") or key)
            name = row["name"].strip()
            route_id = stable_id(city_id, key)
            db.execute("INSERT OR IGNORE INTO routes VALUES (?,?,?,?,?,?,?)",
                       (route_id, city_id, code, name, "Otobüs", None, normalize(f"{code} {name}")))
        for row in catalog.get("stops", []):
            code = str(row["stopId"])
            name = row["name"].strip()
            lat, lon = coordinates(row.get("lat"), row.get("lng"))
            db.execute("INSERT OR IGNORE INTO stops VALUES (?,?,?,?,?,?,?,?)",
                       (stable_id(city_id, code), city_id, code, name, lat, lon, None,
                        normalize(f"{code} {name}")))


def add_ego_schedules(db):
    with gzip.open(ASSETS / "ego-schedules.dbz", "rb") as source:
        stream = BytesIO(source.read())
    count, = struct.unpack(">I", stream.read(4))
    for _ in range(count):
        key_size, = struct.unpack(">H", stream.read(2))
        key = stream.read(key_size).decode()
        size, = struct.unpack(">I", stream.read(4))
        value = stream.read(size).decode()
        schedule = json.loads(value)
        display = {field: schedule[field] for field in
                   ("days", "synced_at", "source", "origin", "destination") if field in schedule}
        db.execute("INSERT INTO ego_schedules VALUES (?,?)",
                   (key, json.dumps(display, ensure_ascii=False, separators=(",", ":"))))
        route = db.execute("SELECT id FROM routes WHERE city=0 AND code=?", (key,)).fetchone()
        if route is None or db.execute("SELECT 1 FROM route_stops WHERE route=? LIMIT 1", route).fetchone():
            continue
        for seq, row in enumerate(schedule.get("stops", [])):
            stop = db.execute("SELECT id FROM stops WHERE city=0 AND code=? ORDER BY id LIMIT 1",
                              (row["code"],)).fetchone()
            if stop is not None:
                db.execute("INSERT INTO route_stops VALUES (?,?,?)", (route[0], stop[0], seq))
    if stream.read(1):
        raise ValueError("Unexpected data after EGO schedules")


def add_planned_data(db):
    for path in sorted((ASSETS / "stop-times").glob("*.dbz")):
        stop_id = int(path.stem)
        with gzip.open(path, "rt", encoding="utf-8") as source:
            departures = json.load(source)
        db.executemany("INSERT INTO departures VALUES (?,?,?,?)",
                       ((stop_id, route, service, time) for route, service, time in departures))
    for path in sorted((ASSETS / "route-shapes").glob("*.dbz")):
        with gzip.open(path, "rt", encoding="utf-8") as source:
            points = json.load(source)
        if any(coordinates(lat, lon)[0] is None for lat, lon in points):
            raise ValueError(f"Invalid shape coordinates in {path.name}")
        packed = b"".join(struct.pack(">ii", round(lat * 10**6), round(lon * 10**6))
                          for lat, lon in points)
        db.execute("INSERT INTO ios_shapes VALUES (?,?)", (int(path.stem), packed))


def build(output):
    output = output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=output.parent) as temp:
        candidate = Path(temp) / output.name
        with sqlite3.connect(f"{(ASSETS / 'transit-v101.sqlite').as_uri()}?mode=ro", uri=True) as original:
            with sqlite3.connect(candidate) as db:
                original.backup(db)
                cities = json.loads((ASSETS / "cities.json").read_text(encoding="utf-8"))
                cities = [c for c in cities if c.get("status") != "unavailable"]
                for city_id, name in [(4, "Muğla"), (5, "Eskişehir")]:
                    if not any(c["id"] == city_id for c in cities):
                        cities.append({"id": city_id, "name": name, "provider": "static"})
                db.executescript("""
                    CREATE TABLE ios_cities(id INTEGER PRIMARY KEY, name TEXT NOT NULL, provider TEXT);
                    CREATE TABLE ego_schedules(code TEXT PRIMARY KEY, json TEXT NOT NULL);
                    CREATE TABLE ios_shapes(route INTEGER PRIMARY KEY, points BLOB NOT NULL);
                    DROP INDEX IF EXISTS stop_geo;
                    DROP INDEX IF EXISTS stop_city;
                    DROP INDEX IF EXISTS route_city;
                    CREATE INDEX ios_routes_city_code ON routes(city,code);
                    CREATE INDEX ios_stops_city_code ON stops(city,code);
                    CREATE INDEX ios_stops_city_lat ON stops(city,lat);
                """)
                db.executemany("INSERT INTO ios_cities VALUES (?,?,?)",
                               ((c["id"], c["name"], c.get("provider")) for c in cities))
                for table in ("stops", "routes"):
                    db.executemany(f"UPDATE {table} SET search=? WHERE id=?",
                                   ((normalize(f"{code} {name}"), row_id)
                                    for row_id, code, name in db.execute(f"SELECT id,code,name FROM {table}")))
                add_regional_catalogues(db, cities)
                add_ego_schedules(db)
                add_planned_data(db)
                db.commit()
                db.execute("VACUUM")
                if db.execute("PRAGMA quick_check").fetchone() != ("ok",):
                    raise RuntimeError("iOS catalogue failed SQLite integrity check")
            db.close()
        candidate.replace(output)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: build_catalog.py OUTPUT.sqlite")
    build(Path(sys.argv[1]))
