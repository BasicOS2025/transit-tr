"""Record final appended catalogue counts and raw-file hashes, without changing data IDs."""
from pathlib import Path
import sqlite3,json,hashlib
ROOT=Path(__file__).resolve().parents[1]
path=ROOT/'data/manifest.json';value=json.loads(path.read_text(encoding='utf-8'))
with sqlite3.connect(ROOT/'android-legacy/assets/transit-v101.sqlite') as db:
    for city in value['cities']:
        for table in ['routes','stops']:city[table]=db.execute('SELECT count(*) FROM '+table+' WHERE city=?',(city['id'],)).fetchone()[0]
    value['sources']=[dict(zip(['id','name','url','license','downloaded','end'],row)) for row in db.execute('SELECT * FROM sources ORDER BY id')]
value['date']='2026-11-07'
value['snapshots']={'ankara_osm_coordinates':'2026-11-07','ego_schedule_sync':'2026-11-07','izmir_bus_csv':'2026-11-07','konya_gtfs':'2025-11'}
value['raw_sha256']={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((ROOT/'data/raw').iterdir()) if p.is_file()}
path.write_text(json.dumps(value,ensure_ascii=False,indent=2),encoding='utf-8')
