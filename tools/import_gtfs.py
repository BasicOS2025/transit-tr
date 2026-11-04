#!/usr/bin/env python3
"""Extract bounded static route/stop catalog; never interpret it as live ETA."""
import argparse,csv,io,json,zipfile,hashlib,datetime
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('zip');p.add_argument('--city',required=True);p.add_argument('--source',required=True);p.add_argument('--license',required=True);p.add_argument('--out',required=True);a=p.parse_args()
with zipfile.ZipFile(a.zip) as z:
 def rows(name):
  info=z.getinfo(name)
  if info.file_size>8*1024*1024: raise ValueError('catalog file exceeds 8 MiB')
  return list(csv.DictReader(io.StringIO(z.read(name).decode('utf-8-sig'))))
 routes=rows('routes.txt');stops=rows('stops.txt');feed=rows('feed_info.txt') if 'feed_info.txt' in z.namelist() else []
 if len(routes)>3000 or len(stops)>30000:raise ValueError('catalog too large; split by operator')
 result={'city':a.city,'live':False,'source':a.source,'license':a.license,'retrieved_at':datetime.date.today().isoformat(),'sha256':hashlib.sha256(Path(a.zip).read_bytes()).hexdigest(),'feed_info':feed,'routes':[[r.get('route_short_name') or r['route_id'],r.get('route_long_name',''),r.get('route_type','')] for r in routes],'stops':[[r['stop_name'],r['stop_id'],r.get('stop_lat',''),r.get('stop_lon','')] for r in stops]}
 Path(a.out).write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
