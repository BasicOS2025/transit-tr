"""Export the same native catalogue, metro and compressed schedules for the standalone web service."""
from pathlib import Path
import sqlite3,json,gzip,shutil
root=Path(__file__).resolve().parents[1];assets=root/'android-legacy/assets';out=root/'web-service/data';out.mkdir(parents=True,exist_ok=True)
db=sqlite3.connect(assets/'transit-v101.sqlite');db.row_factory=sqlite3.Row
for city in [0,1,2,6]:
 routes=[dict(r) for r in db.execute('select * from routes where city=?',(city,))]
 stops=[dict(r) for r in db.execute('select * from stops where city=?',(city,))]
 ids={r['id'] for r in routes};rs={};external={}
 for route,stop,seq in db.execute('select route,stop,seq from route_stops order by route,seq'):
  if route in ids:rs.setdefault(str(route),[]).append(stop)
 for route,code in db.execute('select route,code from route_external'):
  if route in ids:external[str(route)]=code
 data={'routes':routes,'stops':stops,'routeStops':rs,'external':external,'sources':[dict(r) for r in db.execute('select * from sources')]}
 (out/('catalog-'+str(city)+'.json.gz')).write_bytes(gzip.compress(json.dumps(data,ensure_ascii=False,separators=(',',':')).encode(),mtime=0))
for name in ['cities.json','bursa-bootstrap.json','rail-networks.json','rail-estimates.json','city-catalog-3.dbz','city-catalog-7.dbz','city-catalog-8.dbz','city-catalog-9.dbz']:
 shutil.copyfile(assets/name,out/name)
for p in assets.glob('city-catalog-1*.dbz'):shutil.copyfile(p,out/p.name)
shutil.copyfile(assets/'catalog-meta.json',out/'catalog-meta.json')
shutil.copyfile(assets/'ego-schedules.dbz',out/'ego-schedules.dbz')
for directory in ['route-shapes','stop-times']:shutil.copytree(assets/directory,out/directory,dirs_exist_ok=True)
print('Standalone transport data exported:',len(list(out.rglob('*'))),'files')
