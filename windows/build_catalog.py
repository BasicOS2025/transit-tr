#!/usr/bin/env python3
"""Export the shared Transit TR catalogue for the Windows desktop client."""
import gzip
from contextlib import closing
import json
import sqlite3
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def write(path, data):
    payload = json.dumps(data, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    path.write_bytes(gzip.compress(payload, mtime=0))


def export(db, folder):
    cities = [dict(city) for city in db.execute("SELECT id,name,provider FROM ios_cities ORDER BY id")]
    write(folder / "cities.json.gz", cities)
    for city in cities:
        key = city["id"]
        routes = [dict(row) for row in db.execute(
            "SELECT id,code,name,type FROM routes WHERE city=? ORDER BY code,id", (key,))]
        stops = [dict(row) for row in db.execute(
            "SELECT id,code,name,lat,lon FROM stops WHERE city=? ORDER BY code,id", (key,))]
        links = {}
        for route, stop in db.execute("""
            SELECT r.id,l.stop FROM routes r JOIN route_stops l ON l.route=r.id
            WHERE r.city=? ORDER BY r.id,l.seq
            """, (key,)):
            links.setdefault(str(route), []).append(stop)
        schedules = {code: json.loads(value) for code, value in db.execute(
            "SELECT code,json FROM ego_schedules")} if key == 0 else {}
        write(folder / f"{key}.json.gz", dict(routes=routes, stops=stops, links=links, schedules=schedules))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: build_catalog.py OUTPUT_DIRECTORY")
    folder = Path(sys.argv[1])
    folder.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as temporary:
        database = Path(temporary) / "transit.sqlite"
        subprocess.run([sys.executable, ROOT / "ios/build_catalog.py", database], check=True)
        with closing(sqlite3.connect(database)) as db:
            db.row_factory = sqlite3.Row
            export(db, folder)
