"""Synchronize published EGO website timetable facts. No third-party code execution."""
import argparse, concurrent.futures, hashlib, html, json, re, threading, time, urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def clean(value):return re.sub(r'\s+',' ',html.unescape(re.sub(r'<[^>]*>',' ',value))).strip()
def parse(page,code,at=None):
    marker=page.find('<!-- Saat Tablosu -->')
    if marker<0:raise ValueError('Timetable marker missing')
    header=page[:marker];meta={}
    for row in re.findall(r'<tr[^>]*>(.*?)</tr>',header,re.S|re.I):
        values=[clean(v) for v in re.findall(r'<td[^>]*>(.*?)</td>',row,re.S|re.I)]
        if len(values)>=3:meta[values[0]]=values[2]
    if meta.get('Hat No')!=code:raise ValueError('Line identity mismatch')
    body=page[marker:];cells=re.findall(r'<td[^>]*class=["\'][^"\']*hs-times[^"\']*["\'][^>]*>(.*?)</td>',body,re.S|re.I)
    if len(cells)!=3:raise ValueError('Three day columns required')
    days=[]
    for cell in cells:
        values=[]
        for part in re.split(r'<br\s*/?>',cell,flags=re.I):
            text=clean(part)
            if re.match(r'^\d{2}:\d{2}(?:\s|$)',text):values.append(text.rstrip(' -'))
        days.append(values)
    stops=[];route_start=body.find('<!-- Geçtiği Güzergahlar -->')
    if route_start>=0:
        for row in re.findall(r'<tr[^>]*>(.*?)</tr>',body[route_start:],re.S|re.I):
            values=[clean(v) for v in re.findall(r'<td[^>]*>(.*?)</td>',row,re.S|re.I)]
            if len(values)>=3 and values[0].isdigit() and re.fullmatch(r'[A-Za-z0-9_-]{1,20}',values[1]):stops.append({'seq':int(values[0]),'code':values[1],'name':values[2]})
    duration=re.match(r'(\d+)',meta.get('Süresi',''))
    return {'code':code,'name':meta.get('Hat Adı',''),'origin':meta.get('Kalkış Yeri',''),'destination':meta.get('Varış Yeri',''),'duration_minutes':int(duration.group(1)) if duration else None,'synced_at':at or int(time.time()*1000),'source':'https://www.ego.gov.tr/hareketsaatleri?hat_no='+code,'days':days,'stops':stops}
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--cache',type=Path,required=True);parser.add_argument('--workers',type=int,default=2);parser.add_argument('--codes',nargs='*');args=parser.parse_args()
    codes=args.codes or [r['code'] for r in json.loads((ROOT/'data/raw/ankara-lines.json').read_text(encoding='utf-8'))]
    output=ROOT/'android-legacy/assets/ego-times';output.mkdir(parents=True,exist_ok=True);args.cache.mkdir(parents=True,exist_ok=True)
    lock=threading.Lock();done=[];failed=[]
    def fetch(code):
        target=output/(code+'.json');raw=args.cache/(code+'.html')
        try:
            if raw.exists() and time.time()-raw.stat().st_mtime<86400:page=raw.read_text(encoding='utf-8')
            else:
                last=None
                for attempt in range(3):
                    try:
                        time.sleep(.25);req=urllib.request.Request('https://www.ego.gov.tr/hareketsaatleri?hat_no='+code,headers={'User-Agent':'TransitTR/1.0.1 (published timetable sync)'})
                        with urllib.request.urlopen(req,timeout=20) as response:data=response.read(1048577)
                        if len(data)>1048576:raise ValueError('Page too large')
                        page=data.decode('utf-8');parse(page,code);raw.write_text(page,encoding='utf-8');break
                    except Exception as e:last=e;time.sleep(attempt+1)
                else:raise last
            record=parse(page,code,int(raw.stat().st_mtime*1000));target.write_text(json.dumps(record,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
            with lock:done.append(code)
        except Exception as e:
            with lock:failed.append({'code':code,'error':str(e)})
        with lock:
            count=len(done)+len(failed)
            if count%25==0 or count==len(codes):print('EGO sync',count,'/',len(codes),'OK',len(done),'FAIL',len(failed),flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=max(1,min(2,args.workers))) as pool:list(pool.map(fetch,codes))
    report={'date':time.strftime('%Y-%m-%d'),'requested':len(codes),'synced':len(done),'failures':failed,'source':'https://www.ego.gov.tr/hareketsaatleri','method':'Published three-day timetable and ordered stop table; notes retained'}
    (ROOT/'data/raw/ego-sync-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(report,ensure_ascii=True),flush=True)
if __name__=='__main__':main()
