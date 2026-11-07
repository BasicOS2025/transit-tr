"""Append official ESHOT records without changing existing catalogue/favourite IDs."""
from pathlib import Path
import csv, json, sqlite3, re
ROOT=Path(__file__).resolve().parents[1]; RAW=ROOT/'data/raw'
def rows(name):
    with (RAW/(name+'.csv')).open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f,delimiter=';'))
def normal(s):return s.lower().translate(str.maketrans('ıİşğüöç','iIsguoc')).replace('i̇','i')
db=sqlite3.connect(ROOT/'android-legacy/assets/transit-v101.sqlite')
source=9
db.execute('INSERT OR REPLACE INTO sources VALUES(?,?,?,?,?,?)',(source,'İzmir Büyükşehir Belediyesi / ESHOT','https://acikveri.bizizmir.com/tr/dataset/otobus-hat-guzergahlari','İzmir Büyükşehir Belediyesi Açık Veri Lisansı — https://acikveri.bizizmir.com/tr/license','2026-11-07',''))
stopid=db.execute('SELECT max(id) FROM stops').fetchone()[0];routeid=db.execute('SELECT max(id) FROM routes').fetchone()[0]
known={code:id for id,code in db.execute('SELECT id,code FROM stops WHERE city=2')};newstops=[]
for s in rows('eshot-otobus-duraklari'):
    code=s['DURAK_ID'];lat=float(s['ENLEM']);lon=float(s['BOYLAM']);name=s['DURAK_ADI']
    if code not in known:
        stopid+=1;known[code]=stopid;newstops.append(dict(id=stopid,code=code,name=name,lat=lat,lon=lon,source=source))
        db.execute('INSERT INTO stops VALUES(?,?,?,?,?,?,?,?)',(stopid,2,code,name,lat,lon,source,normal(code+' '+name)))
metadata={s['HAT_NO']:s for s in rows('eshot-otobus-hat-listesi')};shapes={}
for p in rows('otobus-hat-guzergahlari'):
    shapes.setdefault((p['HAT_NO'],p['YON']),[]).append((float(p['ENLEM']),float(p['BOYLAM'])))
newroutes=[];pointcount=0
existing={(code,name):id for id,code,name in db.execute('SELECT id,code,name FROM routes WHERE city=2')}
for (code,direction),points in shapes.items():
    # The official file has coarse rounded coordinates. Preserve its order and precision.
    name=metadata.get(code,{}).get('HAT_ADI',code)+' · '+('Gidiş' if direction=='1' else 'Dönüş' if direction=='2' else 'Yön '+direction)
    if (code,name) in existing:continue
    routeid+=1;newroutes.append(dict(id=routeid,code=code,name=name,type='Otobüs',source=source,stops=[]))
    db.execute('INSERT INTO routes VALUES(?,?,?,?,?,?,?)',(routeid,2,code,name,'Otobüs',source,normal(code+' '+name)))
    previous=None;seq=0
    for lat,lon in points:
        if not (35<=lat<=43 and 25<=lon<=45) or previous==(lat,lon):continue
        db.execute('INSERT INTO route_shapes VALUES(?,?,?,?)',(routeid,seq,lat,lon));seq+=1;previous=(lat,lon)
    pointcount+=seq
assert db.execute('pragma integrity_check').fetchone()[0]=='ok';db.commit();db.close()
web=ROOT/'web/data/2.json';value=json.loads(web.read_text(encoding='utf-8'));value['routes'].extend(newroutes);value['stops'].extend(newstops);web.write_text(json.dumps(value,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print(json.dumps(dict(izmir_added_routes=len(newroutes),izmir_added_stops=len(newstops),izmir_shape_points=pointcount)))
