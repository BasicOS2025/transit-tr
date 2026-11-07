#!/usr/bin/env python3
"""Build reproducible compact catalogs and SQLite assets from licensed raw data.
No network access. Local static schedules are never live arrival predictions.
"""
from pathlib import Path
import csv,io,json,zipfile,sqlite3,re,hashlib
ROOT=Path(__file__).resolve().parents[1];RAW=ROOT/'data/raw'
CITIES=['Ankara','İstanbul','İzmir','Kocaeli','Muğla','Eskişehir','Konya']
URLS=['https://www.ego.gov.tr/','https://iett.istanbul/','https://www.eshot.gov.tr/','https://www.e-komobil.com/','https://www.mugla.bel.tr/','https://www.estram.com.tr/','https://atus.konya.bel.tr/']
catalogs=[dict(id=i,name=n,official=URLS[i],routes=[],stops=[],services=[],departures=[],sources=[]) for i,n in enumerate(CITIES)]
ids={'route':0,'stop':0};sources=[]
def new(k): ids[k]+=1;return ids[k]
def norm(s):return s.lower().translate(str.maketrans('ıİşğüöçâîû','iIsguocaiu')).replace('i̇','i')
def source(city,name,url,license,end=''):
 s=dict(id=len(sources)+1,name=name,url=url,license=license,downloaded='2026-11-04',end=end);sources.append(s);catalogs[city]['sources'].append(s);return s['id']
def rows(raw):return list(csv.DictReader(io.StringIO(raw.decode('utf-8-sig')),delimiter=';' if raw.count(b';')>raw.count(b',') else ','))
def fixname(s):
 for _ in range(2):
  if any(c in s for c in 'ÃÄÅ'):
   try:s=b''.join(bytes([ord(ch)]) if ord(ch)<256 else ch.encode('cp1252') for ch in s).decode('utf8')
   except (UnicodeError,LookupError):break
 return s.strip()
def coord(s):
 s=str(s or '').replace(',','.')
 if s.count('.')>1:
  d=s.replace('.','');s=d[:2]+'.'+d[2:]
 try:return float(s)
 except ValueError:return None
# Istanbul bus data, officially distributed GTFS-shaped CSV files.
c=catalogs[1];src=source(1,'İBB / İETT','https://data.ibb.gov.tr/dataset/iett-gtfs-verisi','İBB Açık Veri Lisansı https://data.ibb.gov.tr/license')
seen=set()
for r in rows((RAW/'iett-routes.csv').read_bytes()):
 code=fixname(r['route_short_name']);name=fixname(r['route_long_name']);key=(code,name)
 if not code or not name:continue
 if key in seen:continue
 seen.add(key);c['routes'].append(dict(id=new('route'),code=code,name=name,type='Otobüs',source=src,stops=[]))
for r in rows((RAW/'iett-stops.csv').read_bytes()):
 lat,lon=coord(r['stop_lat']),coord(r['stop_lon'])
 if lat is None or lon is None or not 35<lat<43 or not 25<lon<45:continue
 c['stops'].append(dict(id=new('stop'),code=r['stop_code'],name=fixname(r['stop_name']),lat=lat,lon=lon,source=src))
# Istanbul rail API is explicitly listed by IBB open-data catalog.
meta=json.loads((RAW/'metro-meta.json').read_text(encoding='utf-8'))['result']['results']
assert any(any('GetStations' in x['url'] for x in d['resources']) for d in meta)
src=source(1,'İBB / Metro İstanbul','https://api.ibb.gov.tr/MetroIstanbul/api/MetroMobile/V2/GetStations','İBB Açık Veri Lisansı https://data.ibb.gov.tr/license')
lines={x['Id']:x for x in json.loads((RAW/'istanbul-lines.json').read_text(encoding='utf-8'))['Data']}
groups={}
for r in json.loads((RAW/'istanbul-stations.json').read_text(encoding='utf-8'))['Data']:
 p=r.get('DetailInfo') or {};lat,lon=coord(p.get('Latitude','')),coord(p.get('Longitude',''))
 if lat is None or lon is None or not 35<lat<43 or not 25<lon<45:continue
 stop=dict(id=new('stop'),code=str(r['Id']),name=r.get('Description') or r['Name'],lat=lat,lon=lon,source=src);c['stops'].append(stop);groups.setdefault(r['LineId'],[]).append((r.get('Order',0),stop))
for lid,stations in groups.items():
 l=lines.get(lid,{});code=l.get('Name',str(lid));name=l.get('Description') or (stations[0][1]['name']+' – '+stations[-1][1]['name']);name=re.sub('<[^>]+>','',name)
 c['routes'].append(dict(id=new('route'),code=code,name=name,type='Teleferik' if code.startswith('TF') else 'Tramvay' if code.startswith('T') else 'Füniküler' if code.startswith('F') else 'Raylı sistem',source=src,stops=[s['id'] for _,s in sorted(stations,key=lambda p:p[0])]))
# Three Izmir GTFS feeds; archive includes full input for reproducibility.
for basename,title,url in [('metro','Metro','https://www.izmirmetro.com.tr/gtfs/rail-metro-gtfs.zip'),('tram','Tramvay','https://www.tramizmir.com/gtfs/rail-tramizmir-gtfs.zip'),('izban','İZBAN (eski katalog)','https://www.izban.com.tr/gtfs/rail-izban-gtfs.zip')]:
 c=catalogs[2];z=zipfile.ZipFile(RAW/('izmir-'+basename+'.zip'))
 def read(n):return rows(z.read(n)) if n in z.namelist() else []
 calendars=read('calendar.txt');end=max((x['end_date'] for x in calendars),default='');src=source(2,'İzmir Büyükşehir Belediyesi / '+title,url,'CC BY 4.0 https://creativecommons.org/licenses/by/4.0/',end)
 stops={};routes={};trips={}
 for r in read('stops.txt'):
  lat,lon=coord(r['stop_lat']),coord(r['stop_lon'])
  if lat is None or lon is None:continue
  x=dict(id=new('stop'),code=r['stop_id'],name=r['stop_name'],lat=lat,lon=lon,source=src);stops[r['stop_id']]=x;c['stops'].append(x)
 for r in read('routes.txt'):
  x=dict(id=new('route'),code=r['route_short_name'] or r['route_id'],name=r['route_long_name'],type=title,source=src,stops=[]);routes[r['route_id']]=x;c['routes'].append(x)
 exceptions={}
 for r in read('calendar_dates.txt'):exceptions.setdefault(r['service_id'],{})[r['date']]=int(r['exception_type'])
 services={}
 for r in calendars:
  sid=str(src)+':'+r['service_id'];services[r['service_id']]=sid;c['services'].append(dict(id=sid,start=r['start_date'],end=r['end_date'],mask=sum(int(r[k])<<i for i,k in enumerate(['monday','tuesday','wednesday','thursday','friday','saturday','sunday'])),exceptions=exceptions.get(r['service_id'],{})))
 for rawsid,ex in exceptions.items():
  if rawsid not in services:
   sid=str(src)+':'+rawsid;services[rawsid]=sid;c['services'].append(dict(id=sid,start='00000000',end='00000000',mask=0,exceptions=ex))
 for r in read('trips.txt'):
  if r['route_id'] in routes:trips[r['trip_id']]=(routes[r['route_id']]['id'],services.get(r['service_id'],''))
 seq={};deps=set()
 for r in read('stop_times.txt'):
  if r['trip_id'] not in trips or r['stop_id'] not in stops:continue
  rid,service=trips[r['trip_id']];sid=stops[r['stop_id']]['id'];seq.setdefault(r['trip_id'],[]).append((int(r['stop_sequence']),sid))
  if service and r.get('departure_time'):
   h,m,s=map(int,r['departure_time'].split(':'));deps.add((sid,rid,service,h*3600+m*60+s))
 for r in routes.values():
  candidates=[v for tid,v in seq.items() if trips[tid][0]==r['id']]
  if candidates:r['stops']=[sid for _,sid in sorted(max(candidates,key=len))]
 c['departures'].extend(sorted(deps))
# Konya: catalog and representative route sequence only; full bus schedules
# deliberately stay out of the single-file browser catalog to bound its RAM.
c=catalogs[6];z=zipfile.ZipFile(RAW/'konya-gtfs.zip')
def kr(n):return rows(z.read(n))
end=max(r['end_date'] for r in kr('calendar.txt'))
src=source(6,'Konya Büyükşehir Belediyesi (GTFS 11/2025)','https://acikveri.konya.bel.tr/dataset/toplu-tasima-gtfs-verileri','CC BY 4.0 https://creativecommons.org/licenses/by/4.0/',end)
ks={};krs={}
for r in kr('stops.txt'):
 x=dict(id=new('stop'),code=r['stop_id'],name=r['stop_name'],lat=float(r['stop_lat']),lon=float(r['stop_lon']),source=src);ks[r['stop_id']]=x;c['stops'].append(x)
for r in kr('routes.txt'):
 x=dict(id=new('route'),code=r['route_short_name'],name=r['route_long_name'],type='Tramvay' if r['route_type']=='0' else 'Otobüs',source=src,stops=[]);krs[r['route_id']]=x;c['routes'].append(x)
kt={};chosen={}
for r in kr('trips.txt'):
 if r['route_id'] not in chosen and r['route_id'] in krs:chosen[r['route_id']]=r['trip_id'];kt[r['trip_id']]=r['route_id']
sequences={}
with z.open('stop_times.txt') as stream:
 for r in csv.DictReader(io.TextIOWrapper(stream,encoding='utf-8-sig')):
  if r['trip_id'] in kt and r['stop_id'] in ks:sequences.setdefault(kt[r['trip_id']],[]).append((int(r['stop_sequence']),ks[r['stop_id']]['id']))
for rid,seq in sequences.items():krs[rid]['stops']=[sid for _,sid in sorted(seq)]
# Ankara: official line catalog and egomac's attributed OSM stop-name snapshot.
c=catalogs[0]
src=source(0,'EGO resmî hat kataloğu','https://www.ego.gov.tr/hareketsaatleri','Resmî hizmet; veri lisansı belirtilmemiş')
for r in json.loads((RAW/'ankara-lines.json').read_text(encoding='utf-8')):
 c['routes'].append(dict(id=new('route'),code=r['code'],name=r['name'],type=r['type'],source=src,stops=[]))
src=source(0,'Ankara durakları · EGO adları / OpenStreetMap koordinatları, 2026-11-07','https://www.openstreetmap.org/copyright','© OpenStreetMap contributors, ODbL; EGO yayımlanmış durak adları; eski ad girdisi egomac MIT')
seen_stops=set()
for r in json.loads((RAW/'ankara-stops-osm.json').read_text(encoding='utf-8'))['stops']:
 code=str(r.get('r','')).strip();name=r.get('n','').strip()
 if not re.fullmatch(r'(?:[0-9]{5}|[MA][0-9]{1,3})',code) or not name or code in seen_stops:continue
 seen_stops.add(code);c['stops'].append(dict(id=new('stop'),code=code,name=name,lat=r.get('lat'),lon=r.get('lon'),source=src))

# Output SQLite optimized for old Android's built-in SQLite, plus browser JSON.
asset=ROOT/'android-legacy/assets/transit-v101.sqlite';asset.unlink(missing_ok=True);db=sqlite3.connect(asset)
db.executescript('''CREATE TABLE routes(id INTEGER PRIMARY KEY,city INTEGER,code TEXT,name TEXT,type TEXT,source INTEGER,search TEXT);
CREATE TABLE stops(id INTEGER PRIMARY KEY,city INTEGER,code TEXT,name TEXT,lat REAL,lon REAL,source INTEGER,search TEXT);
CREATE TABLE route_stops(route INTEGER,stop INTEGER,seq INTEGER);
CREATE TABLE sources(id INTEGER PRIMARY KEY,name TEXT,url TEXT,license TEXT,downloaded TEXT,end TEXT);
CREATE TABLE services(id TEXT PRIMARY KEY,start TEXT,end TEXT,mask INTEGER);
CREATE TABLE exceptions(service TEXT,date TEXT,type INTEGER);
CREATE TABLE departures(stop INTEGER,route INTEGER,service TEXT,time INTEGER);
CREATE INDEX rs_route ON route_stops(route,seq); CREATE INDEX stop_geo ON stops(lat,lon); CREATE INDEX stop_city ON stops(city);
CREATE INDEX route_city ON routes(city);CREATE INDEX d_stop ON departures(stop,time);CREATE INDEX ex_service ON exceptions(service,date);
''')
for s in sources:db.execute('INSERT INTO sources VALUES(?,?,?,?,?,?)',(s['id'],s['name'],s['url'],s['license'],s['downloaded'],s['end']))
db.executescript('CREATE TABLE IF NOT EXISTS route_external(route INTEGER PRIMARY KEY,code TEXT); CREATE TABLE IF NOT EXISTS route_shapes(route INTEGER,seq INTEGER,lat REAL,lon REAL); CREATE INDEX IF NOT EXISTS idx_shapes_route ON route_shapes(route,seq);')
for c in catalogs:
 for r in c['routes']:
  db.execute('INSERT INTO routes VALUES(?,?,?,?,?,?,?)',(r['id'],c['id'],r['code'],r['name'],r['type'],r['source'],norm(r['code']+' '+r['name'])))
  db.executemany('INSERT INTO route_stops VALUES(?,?,?)',[(r['id'],s,i) for i,s in enumerate(r['stops'])])
 for s in c['stops']:db.execute('INSERT INTO stops VALUES(?,?,?,?,?,?,?,?)',(s['id'],c['id'],s['code'],s['name'],s['lat'],s['lon'],s['source'],norm(s['name']+' '+s['code'])))
 for s in c['services']:
  db.execute('INSERT INTO services VALUES(?,?,?,?)',(s['id'],s['start'],s['end'],s['mask']));db.executemany('INSERT INTO exceptions VALUES(?,?,?)',[(s['id'],d,t) for d,t in s['exceptions'].items()])
 db.executemany('INSERT INTO departures VALUES(?,?,?,?)',c['departures'])
 path=ROOT/'web/data';path.mkdir(exist_ok=True);(path/(str(c['id'])+'.json')).write_text(json.dumps(c,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
 print(c['name'],len(c['routes']),'routes',len(c['stops']),'stops',len(c['departures']),'scheduled departures')
db.commit();db.execute('VACUUM');db.close()
(ROOT/'data/manifest.json').write_text(json.dumps(dict(date='2026-11-04',cities=[dict(id=c['id'],name=c['name'],official=c['official'],routes=len(c['routes']),stops=len(c['stops'])) for c in catalogs],sources=sources,raw_sha256={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(RAW.iterdir())}),ensure_ascii=False,indent=2),encoding="utf-8")
