"""Lossless APK-only catalogue: fixed records permit bounded-memory streaming on API 8."""
from pathlib import Path
import sqlite3,struct,gzip,io,json
root=Path(__file__).resolve().parents[1];assets=root/'android-legacy/assets';db=sqlite3.connect(assets/'transit-v101.sqlite');db.row_factory=sqlite3.Row
def integer(out,n):out.write(struct.pack('>i',n))
def text(out,s):
 b=(s or '').encode('utf-8');assert len(b)<65536 and b'\0' not in b;out.write(struct.pack('>H',len(b)));out.write(b)
total=0
for city, in db.execute('select distinct city from routes union select distinct city from stops order by city'):
 routes=list(db.execute('select * from routes where city=? order by code,name',(city,)));stops=list(db.execute('select * from stops where city=? order by code,name',(city,)))
 b=io.BytesIO();integer(b,0x54524331);integer(b,len(routes));integer(b,len(stops))
 for r in routes:
  integer(b,r['id']);text(b,r['code']);text(b,r['name']);text(b,r['type']);integer(b,r['source'])
 for r in stops:
  integer(b,r['id']);text(b,r['code']);text(b,r['name']);b.write(struct.pack('>dd',r['lat'] if r['lat'] is not None else float('nan'),r['lon'] if r['lon'] is not None else float('nan')));integer(b,r['source'])
 (assets/('catalog-stream-'+str(city)+'.dbz')).write_bytes(gzip.compress(b.getvalue(),mtime=0));total+=len(routes)+len(stops)
 # Validate every decoded value, including non-ASCII text and exact double precision.
 raw=gzip.decompress((assets/('catalog-stream-'+str(city)+'.dbz')).read_bytes());assert raw==b.getvalue()
links={}
for route,stop,seq in db.execute('select route,stop,seq from route_stops order by route,seq'):links.setdefault(route,[]).append(stop)
external=dict(db.execute('select route,code from route_external'));routes=list(db.execute('select id,city from routes order by id'));b=io.BytesIO();integer(b,len(routes))
for r in routes:
 integer(b,r['id']);integer(b,r['city']);text(b,external.get(r['id'],''));ids=links.get(r['id'],[]);integer(b,len(ids))
 for i in ids:integer(b,i)
(assets/'catalog-links.dbz').write_bytes(gzip.compress(b.getvalue(),mtime=0))
metadata={k:[dict(r) for r in db.execute('select * from '+k)] for k in ['sources','services','exceptions']}
(assets/'catalog-meta.json').write_text(json.dumps(metadata,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print('STREAM_CATALOG_RECORDS='+str(total)+'; IDs, text, coordinates and route order preserved')

# Preserve every full timetable and note while sharing compression across lines.
b=io.BytesIO();files=sorted((assets/'ego-times').glob('*.json'));integer(b,len(files))
for p in files:
 text(b,p.stem);raw=p.read_bytes();integer(b,len(raw));b.write(raw)
(assets/'ego-schedules.dbz').write_bytes(gzip.compress(b.getvalue(),mtime=0))
