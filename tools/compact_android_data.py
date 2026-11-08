"""Keep exact route coordinates and timetables in per-item compressed assets.
SQLite retains catalogue IDs, names, coordinates and relationships, without duplicate search text.
"""
from pathlib import Path
import sqlite3,json,gzip
ROOT=Path(__file__).resolve().parents[1];ASSETS=ROOT/'android-legacy/assets'
db=sqlite3.connect(ASSETS/'transit-v101.sqlite')
counts={}
for table,folder,key,columns,order in [
    ('route_shapes','route-shapes','route','lat,lon','seq'),
    ('departures','stop-times','stop','route,service,time','time,route,service')]:
    directory=ASSETS/folder;directory.mkdir(exist_ok=True)
    identifiers=[row[0] for row in db.execute('SELECT DISTINCT '+key+' FROM '+table)]
    for identifier in identifiers:
        values=list(db.execute('SELECT '+columns+' FROM '+table+' WHERE '+key+'=? ORDER BY '+order,(identifier,)))
        payload=json.dumps(values,ensure_ascii=False,separators=(',',':')).encode('utf-8')
        (directory/(str(identifier)+'.dbz')).write_bytes(gzip.compress(payload,compresslevel=9,mtime=0))
    if identifiers:
        valid={str(v)+'.dbz' for v in identifiers}
        for old in directory.glob('*.dbz'):
            if old.name not in valid:old.unlink()
    counts[folder]=sum(1 for _ in directory.glob('*.dbz'))
    db.execute('DELETE FROM '+table)
db.execute('UPDATE routes SET search=NULL');db.execute('UPDATE stops SET search=NULL')
db.commit();db.execute('VACUUM');assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok';db.close()
counts['sqlite_bytes']=(ASSETS/'transit-v101.sqlite').stat().st_size
counts['compressed_shapes_bytes']=sum(p.stat().st_size for p in (ASSETS/'route-shapes').glob('*.dbz'))
counts['compressed_timetables_bytes']=sum(p.stat().st_size for p in (ASSETS/'stop-times').glob('*.dbz'))
(ROOT/'data/compact-android-report.json').write_text(json.dumps(counts,indent=2),encoding='utf-8')
print(json.dumps(counts))
