"""Combine attributed coordinates and official ordered stops, retaining existing favourite IDs."""
from pathlib import Path
import csv,io,json,re,shutil,sqlite3,subprocess,sys,zipfile
ROOT=Path(__file__).resolve().parents[1];RAW=ROOT/'data/raw'
original=RAW/'ankara-stops-egomac-names.json'
if not original.exists():shutil.copyfile(RAW/'ankara-stops-osm.json',original)
osm=json.loads((RAW/'ankara-osm-20261107.json').read_text(encoding='utf-8'))['elements'];coordinates={};osmnames={}
for node in osm:
    for code in re.split(r'[;,/ ]+',node.get('tags',{}).get('ref','')):
        if re.fullmatch(r'[0-9]{5}',code):
            coordinates.setdefault(code,[]).append((node['lat'],node['lon']))
            if node.get('tags',{}).get('name'):osmnames[code]=node['tags']['name']
coordinates={code:values[0] for code,values in coordinates.items() if len(set(values))==1}
def normal(value):return re.sub(r'\s+',' ',value.lower().translate(str.maketrans('ıİşğüöçâîû','iIsguocaiu')).replace('i̇','i').replace('metro',' ').replace('istasyonu',' ').replace('istasyon',' ')).strip()
rail=json.loads((ROOT/'android-legacy/assets/rail-networks.json').read_text(encoding='utf-8'))['lines'];railnames={normal(s['name']):(s['lat'],s['lon']) for line in rail if line['city']==0 for s in line['stations']}
schedules={};official={};metrocoords={}
for path in (ROOT/'android-legacy/assets/ego-times').glob('*.json'):
    value=json.loads(path.read_text(encoding='utf-8'));schedules[value['code']]=value
    for stop in value['stops']:
        official[stop['code']]=stop['name']
        if stop['code'].startswith(('M','A')) and normal(stop['name']) in railnames:metrocoords[stop['code']]=railnames[normal(stop['name'])]
old=json.loads(original.read_text(encoding='utf-8'))['stops'];merged=[];seen=set()
for stop in old:
    code=str(stop.get('r','')).strip();name=stop.get('n','').strip()
    if not re.fullmatch(r'[0-9]{5}',code) or not name or code in seen:continue
    seen.add(code);merged.append({'r':code,'n':official.get(code,name)})
assert len(merged)==2813
current=json.loads((RAW/'ankara-stops-osm.json').read_text(encoding='utf-8')).get('stops',[])
for stop in current:
    code=str(stop.get('r','')).strip()
    if code in seen or not stop.get('n') or not re.fullmatch(r'(?:[0-9]{5}|[MA][0-9]{1,3})',code):continue
    merged.append({'r':code,'n':official.get(code,stop['n'])});seen.add(code)
for code in sorted(set(coordinates)|set(official)):
    if code in seen or not re.fullmatch(r'(?:[0-9]{5}|[MA][0-9]{1,3})',code):continue
    merged.append({'r':code,'n':official.get(code,osmnames.get(code,'Durak '+code))});seen.add(code)
for stop in merged:
    point=coordinates.get(stop['r'],metrocoords.get(stop['r']))
    if point:stop['lat'],stop['lon']=point
(RAW/'ankara-stops-osm.json').write_text(json.dumps({'date':'2026-11-07','attribution':'EGO published stop names; © OpenStreetMap contributors, ODbL coordinates; egomac MIT names retained','stops':merged},ensure_ascii=False,separators=(',',':')),encoding='utf-8')
previous=ROOT/'android-legacy/assets/transit-v101.sqlite';identities={}
if not previous.exists():previous=ROOT/'android-legacy/assets/transit-v09.sqlite'
if previous.exists():
    olddb=sqlite3.connect(previous)
    for table in ['routes','stops']:identities[table]=olddb.execute('SELECT id,city,code FROM '+table).fetchall()
    olddb.close()
subprocess.run([sys.executable,str(ROOT/'tools/build_catalog.py')],check=True)
asset=ROOT/'android-legacy/assets/transit-v101.sqlite';db=sqlite3.connect(asset)
db.execute('CREATE TABLE IF NOT EXISTS route_external(route INTEGER PRIMARY KEY,code TEXT)')
def fixname(value):
    for _ in range(2):
        if any(c in value for c in 'ÃÄÅ'):
            try:value=b''.join(bytes([ord(c)]) if ord(c)<256 else c.encode('cp1252') for c in value).decode('utf-8')
            except (UnicodeError,LookupError):break
    return value.strip()
raw=(RAW/'iett-routes.csv').read_text(encoding='utf-8-sig');reader=csv.DictReader(io.StringIO(raw),delimiter=';' if raw.count(';')>raw.count(',') else ',')
identifiers={(code,name):identifier for identifier,code,name in db.execute('SELECT id,code,name FROM routes WHERE city=1')};external=set()
for row in reader:
    key=(fixname(row['route_short_name']),fixname(row['route_long_name']));identifier=identifiers.get(key)
    if identifier and identifier not in external:
        external.add(identifier);db.execute('INSERT INTO route_external VALUES(?,?)',(identifier,row.get('route_code','')))
stops={code:identifier for identifier,code in db.execute('SELECT id,code FROM stops WHERE city=0')};catalog=json.loads((ROOT/'web/data/0.json').read_text(encoding='utf-8'))
for route in catalog['routes']:
    data=schedules.get(route['code']);route['stops']=[stops[s['code']] for s in data['stops'] if s['code'] in stops] if data else []
    db.executemany('INSERT INTO route_stops VALUES(?,?,?)',[(route['id'],stop,seq) for seq,stop in enumerate(route['stops'])])
(ROOT/'web/data/0.json').write_text(json.dumps(catalog,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
db.execute('CREATE TABLE IF NOT EXISTS route_shapes(route INTEGER,seq INTEGER,lat REAL,lon REAL)');db.execute('CREATE INDEX IF NOT EXISTS idx_shapes_route ON route_shapes(route,seq)')
# Konya representative trips match the existing catalogue producer's first-trip choice.
with zipfile.ZipFile(RAW/'konya-gtfs.zip') as z:
    def rows(name):return list(csv.DictReader(io.TextIOWrapper(z.open(name),encoding='utf-8-sig')))
    routes=rows('routes.txt');database=list(db.execute('SELECT id,code FROM routes WHERE city=6 ORDER BY id'));assert len(routes)==len(database)
    routeids={r['route_id']:item[0] for r,item in zip(routes,database)};assert all(r['route_short_name']==item[1] for r,item in zip(routes,database))
    chosen={}
    for trip in rows('trips.txt'):
        if trip['route_id'] not in chosen:chosen[trip['route_id']]=trip.get('shape_id','')
    selected=set(chosen.values());shapes={}
    for point in rows('shapes.txt'):
        if point['shape_id'] in selected:shapes.setdefault(point['shape_id'],[]).append((int(point['shape_pt_sequence']),float(point['shape_pt_lat']),float(point['shape_pt_lon'])))
    def simplify(points,epsilon=.00002):
        if len(points)<3:return points
        keep={0,len(points)-1};stack=[(0,len(points)-1)]
        while stack:
            a,b=stack.pop();ax,ay=points[a];bx,by=points[b];dx,dy=bx-ax,by-ay;den=dx*dx+dy*dy;largest=0;index=0
            for i in range(a+1,b):
                px,py=points[i];t=max(0,min(1,((px-ax)*dx+(py-ay)*dy)/den)) if den else 0;distance=(px-ax-t*dx)**2+(py-ay-t*dy)**2
                if distance>largest:largest,index=distance,i
            if largest>epsilon*epsilon:keep.add(index);stack.extend([(a,index),(index,b)])
        return [points[i] for i in sorted(keep)]
    points_count=0
    for raw,shape in chosen.items():
        points=simplify([(lat,lon) for _,lat,lon in sorted(shapes.get(shape,[]))])
        db.executemany('INSERT INTO route_shapes VALUES(?,?,?,?)',[(routeids[raw],i,lat,lon) for i,(lat,lon) in enumerate(points)]);points_count+=len(points)
assert db.execute('pragma integrity_check').fetchone()[0]=='ok';db.commit();db.execute('VACUUM');db.close()
report={'date':'2026-11-07','ego_lines':len(schedules),'ankara_stops':len(merged),'ankara_coordinate_records':sum('lat' in s for s in merged),'konya_shape_points':points_count,'favourite_ids_preserved':True,'osm_query':'Ankara administrative boundary, highway=bus_stop, ref'}
(RAW/'route-build-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(report),flush=True)

# Official ESHOT bus geometry and stop catalogue, appended after the original IDs.
subprocess.run([sys.executable,str(ROOT/"tools/add_izmir_routes.py")],check=True)

with sqlite3.connect(asset) as finaldb:
    for table,oldrows in identities.items():
        now={r[0]:(r[1],r[2]) for r in finaldb.execute('SELECT id,city,code FROM '+table)}
        assert all(now.get(identifier)==(city,code) for identifier,city,code in oldrows),'Favourite identity changed'

subprocess.run([sys.executable,str(ROOT/"tools/update_route_manifest.py")],check=True)
