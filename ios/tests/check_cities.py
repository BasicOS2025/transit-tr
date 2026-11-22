import concurrent.futures
import json
import sqlite3
import urllib.request, urllib.parse, urllib.error
import time
import sys

if len(sys.argv) != 2:
    raise SystemExit('Usage: check_cities.py CATALOG.sqlite')
db = sqlite3.connect(sys.argv[1])
base = 'https://transit-tr.vercel.app/api/v2/'
examples = {0:'10940',1:'205471',2:'21050',3:'80163',6:'27',8:'12406',9:'3368',1010:'3074'}
cities = db.execute("""
  SELECT id,name,provider,(SELECT count(*) FROM routes WHERE city=c.id),
      (SELECT count(*) FROM stops WHERE city=c.id)
  FROM ios_cities c ORDER BY id
""").fetchall()
work = []
for city, name, provider, routes, stops in cities:
    codes = [r[0] for r in db.execute(
        """SELECT code FROM stops WHERE city=? AND code NOT GLOB '*[^0-9]*' AND length(code) BETWEEN 1 AND 8
           ORDER BY id LIMIT 2""", (city,))]
    work.append((city, name, provider, routes, stops, examples.get(city) or (codes[0] if codes else None)))

def run(item):
    city, name, provider, routes, stops, code = item
    result = {'city':city,'name':name,'routes':routes,'stops':stops,'example':code}
    if not code:
        result['api']='test edilmedi: örnek durak yok' if provider!='static' else 'bağlı değil'
        return result
    url = base+'live?'+urllib.parse.urlencode({'city':city,'stop':code})
    try:
        with urllib.request.urlopen(url, timeout=18) as response:
            data = json.load(response)
        result |= {'api':'yanıt verdi','source':data['source'],'eta':sum(0<=r.get('etaSeconds',-1)<86400 for r in data['rows']),
                   'arrivals':len(data['rows']),'gps':sum(isinstance(r.get('lat'),(int,float)) and isinstance(r.get('lon'),(int,float)) and 0<abs(r['lat'])<85 and 0<abs(r['lon'])<180 for r in data['mapRows'])}
    except urllib.error.HTTPError as e:
        try:
            reason = json.load(e).get('message','')
        except (ValueError,OSError):
            reason = ''
        result['api'] = ('HTTP 502 · kaynak bağlantısı zaman aşımı' if 'ETIMEDOUT' in reason else f'HTTP {e.code}')
    except (KeyError,ValueError,OSError) as e:
        result['api'] = str(e)
    return result

with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    for i in range(0,len(work),4):
        for result in pool.map(run,work[i:i+4]):
            print(json.dumps(result,ensure_ascii=False),flush=True)
        time.sleep(0.3)
