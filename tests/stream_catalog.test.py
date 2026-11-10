"""Check the compact format against the source SQLite, including all text/IDs/doubles/order."""
from pathlib import Path
import sqlite3,gzip,struct,io,math,json
root=Path(__file__).resolve().parents[1];a=root/'android-legacy/assets';db=sqlite3.connect(a/'transit-v101.sqlite');db.row_factory=sqlite3.Row
def integer(f):return struct.unpack('>i',f.read(4))[0]
def text(f):return f.read(struct.unpack('>H',f.read(2))[0]).decode('utf-8')
count=0
for city, in db.execute('select distinct city from routes union select distinct city from stops order by city'):
 f=io.BytesIO(gzip.decompress((a/f'catalog-stream-{city}.dbz').read_bytes()));assert integer(f)==0x54524331;counts=[integer(f),integer(f)]
 for kind,table in enumerate(['routes','stops']):
  rows=list(db.execute(f'select * from {table} where city=? order by code,name',(city,)));assert len(rows)==counts[kind]
  for r in rows:
   assert integer(f)==r['id'];assert text(f)==r['code'];assert text(f)==r['name']
   if kind==0:assert text(f)==r['type']
   else:
    lat,lon=struct.unpack('>dd',f.read(16))
    for actual,expected in [(lat,r['lat']),(lon,r['lon'])]:assert math.isnan(actual) if expected is None else actual==expected
   assert integer(f)==r['source'];count+=1
 assert f.read()==b''
f=io.BytesIO(gzip.decompress((a/'catalog-links.dbz').read_bytes()));n=integer(f);expected=dict(db.execute('select route,code from route_external'))
for i in range(n):
 route=integer(f);city=integer(f);assert db.execute('select city from routes where id=?',(route,)).fetchone()[0]==city;assert text(f)==expected.get(route,'');ids=[integer(f) for _ in range(integer(f))];assert ids==[r[0] for r in db.execute('select stop from route_stops where route=? order by seq',(route,))]
f=io.BytesIO(gzip.decompress((a/'ego-schedules.dbz').read_bytes()));n=integer(f)
for i in range(n):
 code=text(f);raw=f.read(integer(f));assert raw==(a/'ego-times'/f'{code}.json').read_bytes()
assert count==50211
print(f'PASS: {count} catalogue records, every ordered route-stop link and {n} complete EGO timetables preserved; no expanded database required.')
