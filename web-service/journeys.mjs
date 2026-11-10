import {readFileSync} from 'node:fs';
import {gunzipSync} from 'node:zlib';
import path from 'node:path';
import {DATA,cities,catalog,normalize,location,request,kent,bursa,bursaLine,asis} from './transport.mjs';
const read=n=>JSON.parse(readFileSync(path.join(DATA,n),'utf8'));
export const railData=read('rail-networks.json');
const railModel=read('rail-estimates.json'),schedules=new Map();
let bundledTimes;function bundledSchedules(){if(bundledTimes)return bundledTimes;const b=gunzipSync(readFileSync(path.join(DATA,'ego-schedules.dbz'))),map=new Map();let offset=0;const count=b.readInt32BE(offset);offset+=4;for(let i=0;i<count;i++){const length=b.readUInt16BE(offset);offset+=2;const code=b.subarray(offset,offset+length).toString('utf8');offset+=length;const size=b.readInt32BE(offset);offset+=4;map.set(code,JSON.parse(b.subarray(offset,offset+size).toString('utf8')));offset+=size;}bundledTimes=map;return map;}

const clean=s=>String(s).replace(/<[^>]*>/g,' ').replace(/&nbsp;|&#160;/g,' ').replace(/&amp;/g,'&').replace(/&quot;/g,'"').replace(/&#(\d+);/g,(_,n)=>String.fromCodePoint(+n)).replace(/\s+/g,' ').trim();
const station=r=>({code:String(r.stopId||r.stationId||r.Id||r.code||''),name:r.stopName||r.stationName||r.description||r.Title||r.name||'',lat:Number(r.lat??r.latitude??r.Latitude),lon:Number(r.lng??r.longitude??r.Longitude??r.lon)});
export function parseSchedule(html,code,now=Date.now()){
 const marker=html.indexOf('<!-- Saat Tablosu -->');if(marker<0)throw Error('EGO saat tablosu bulunamadı');const header=html.slice(0,marker),body=html.slice(marker);
 const field=label=>clean(new RegExp('<td[^>]*>\\s*'+label+'\\s*</td>\\s*<td[^>]*>\\s*:\\s*</td>\\s*<td[^>]*>(.*?)</td>','is').exec(header)?.[1]||'');if(field('Hat No')!==code)throw Error('EGO hat kimliği eşleşmiyor');
 const days=[...body.matchAll(/<td[^>]*class=["'][^"']*hs-times[^"']*["'][^>]*>(.*?)<\/td>/gis)].map(m=>m[1].split(/<br\s*\/?>/i).map(clean).filter(s=>/^\d\d:\d\d(?:\s.*)?$/.test(s)).map(s=>s.replace(/[ -]+$/,'')));if(days.length!==3)throw Error('EGO gün sütunları okunamadı');
 const stops=[];const start=body.indexOf('<!-- Geçtiği Güzergahlar -->');if(start>=0)for(const row of body.slice(start).matchAll(/<tr[^>]*>(.*?)<\/tr>/gis)){const cols=[...row[1].matchAll(/<td[^>]*>(.*?)<\/td>/gis)].map(m=>clean(m[1]));if(cols.length>=3&&/^\d+$/.test(cols[0])&&/^[A-Za-z0-9_-]{1,20}$/.test(cols[1]))stops.push({seq:+cols[0],code:cols[1],name:cols[2]});}
 return {code,name:field('Hat Adı'),origin:field('Kalkış Yeri'),destination:field('Varış Yeri'),duration_minutes:parseInt(field('Süresi'))||null,days,stops,synced_at:now,source:'https://www.ego.gov.tr/hareketsaatleri?hat_no='+code};
}
export async function schedule(code,force=false){if(!/^[A-Za-z0-9-]{1,16}$/.test(code))throw Error('Geçersiz EGO hat kodu');let old=schedules.get(code);if(!old)try{old=bundledSchedules().get(code);}catch{}if(!force&&old)return old;try{const html=await request('https://www.ego.gov.tr/hareketsaatleri?hat_no='+encodeURIComponent(code),{raw:true});const d=parseSchedule(html,code);schedules.set(code,d);return d;}catch(e){if(!old)throw e;return {...old,stale:true,sync_error:e.message};}}
export const syncJob={running:false,done:0,total:0,errors:0,cancelled:false,lastCode:'',startedAt:0};
export function startSync(){if(syncJob.running)return syncJob;Object.assign(syncJob,{running:true,done:0,total:0,errors:0,cancelled:false,startedAt:Date.now()});const codes=[...bundledSchedules().keys()];syncJob.total=codes.length;(async()=>{try{for(const code of codes){if(syncJob.cancelled)break;syncJob.lastCode=code;try{const d=await schedule(code,true);if(d.stale)syncJob.errors++;}catch{syncJob.errors++;}syncJob.done++;await new Promise(r=>setTimeout(r,300));}}finally{syncJob.running=false;}})();return syncJob;}
export async function catalogPage(city,kind,q='',offset=0){const data=catalog(city);let rows=kind==='stops'?data.stops:data.routes;
 if(city===3||city===8){if(kind==='stops'){const d=await kent(city,'search',{keyword:q});rows=(d.stopList||[]).map(r=>({id:r.stopId,code:r.stopId,name:r.name,lat:Number(r.lat),lon:Number(r.lng)}));}}
 else if(city===9){const d=await bursa('alm/get/searchrouteandstations',{queryParam:q||'19E',vehicleTypes:[],stationTypes:[]});rows=kind==='stops'?(d.stations||[]).map(r=>({...station(r),id:r.stationId})):(d.routes||[]).map(r=>({id:r.routeNo,code:r.routeCode,name:r.routeDescription,routeNo:r.routeNo}));}
 const query=normalize(q);rows=rows.filter(r=>normalize(r.code+' '+r.name).includes(query));return {items:rows.slice(offset,offset+50).map(r=>({...r,external:data.external?.[r.id]||r.external})),total:rows.length,offset,routes:data.routes.length,stops:data.stops.length,onlineStops:[3,8,9].includes(city),message:data.status==='unavailable'?'Bu şehirde kaynak katalog erişimi doğrulanamadı.':''};
}
export async function nearby(city,lat,lon){let stops=catalog(city).stops;
 if(city===9){const d=await bursa('alm/get/nearstations?'+new URLSearchParams({latitude:lat,longitude:lon,radius:2000,count:50}));stops=(d.stations||[]).map(r=>({...station(r),id:r.stationId}));}
 if(city===3||city===8){const d=await kent(city,'near',{lat,lng:lon});stops=(d.busStopList||[]).map(r=>({...station(r),id:r.stopId}));}
 const distance=r=>{const a=Math.PI/180,x=Math.sin((r.lat-lat)*a/2),y=Math.sin((r.lon-lon)*a/2);return 12742000*Math.asin(Math.sqrt(Math.min(1,x*x+Math.cos(lat*a)*Math.cos(r.lat*a)*y*y)));};
 return {items:stops.filter(location).map(r=>({...r,distance:distance(r)})).filter(r=>r.distance<=2000).sort((a,b)=>a.distance-b.distance).slice(0,50)};
}
export function plannedDepartures(city,code,instant=Date.now()){
 const d=catalog(city),stop=d.stops.find(s=>String(s.code)===code),result={items:[],message:'Planlı kalkışlar · canlı varış değildir.'};if(!stop)return result;
 let rows,metadata;try{rows=JSON.parse(gunzipSync(readFileSync(path.join(DATA,'stop-times',stop.id+'.dbz'))));metadata=read('catalog-meta.json');}catch{return result;}
 const routes=new Map(d.routes.map(r=>[r.id,r]));const now=new Date(instant+10800000),sec=now.getUTCHours()*3600+now.getUTCMinutes()*60+now.getUTCSeconds();
 for(let offset=-1;offset<=1;offset++){const date=new Date(+now+offset*86400000),key=date.toISOString().slice(0,10).replaceAll('-',''),bit=1<<((date.getUTCDay()+6)%7),active=new Map(metadata.services.map(s=>[s.id,s.start<=key&&s.end>=key&&!!(s.mask&bit)]));for(const e of metadata.exceptions)if(e.date===key)active.set(e.service,e.type===1);
  for(const [route,service,time] of rows){const delta=time+offset*86400-sec,r=routes.get(route);if(delta<0||delta>=86400||!active.get(service)||!r)continue;result.items.push({delta,time:String(Math.floor(time/3600)%24).padStart(2,'0')+':'+String(Math.floor(time/60)%60).padStart(2,'0'),line:r.code,name:r.name});}
 }result.items.sort((a,b)=>a.delta-b.delta);result.items=result.items.slice(0,8);return result;
}
export async function route(city,code,variant='',direction='G'){
 const data=catalog(city),local=data.routes.find(r=>String(r.code)===code||String(r.id)===code);const out={code,name:local?.name||code,parts:[],stops:[],schematic:false,source:'',notice:''};
 if(city===3||city===8){const d=await kent(city,'route',{displayRouteCode:code,direction:direction==='D'?'1':'0',resultType:'111111',shapeId:'',busStopId:''});const p=d.pathList?.[0];if(!p)throw Error('Kentkart güzergâhı yok');out.parts=[(p.pointList||[]).map(station).filter(location)];out.stops=(p.busStopList||[]).map(station);out.source='Kentkart';
 }else if(city===9){const l=await bursaLine(code);const [p,s]=await Promise.all([bursa('alm/get/routecoordinate?routeNo='+l.routeNo),bursa('alm/get/routestations?RouteNo='+l.routeNo)]);const first=p.routeCoordinates?.find(r=>r.routeDirection===direction)?.routeDirection||p.routeCoordinates?.[0]?.routeDirection;out.parts=[(p.routeCoordinates||[]).filter(r=>r.routeDirection===first).map(station).filter(location)];out.stops=(s.stations||[]).map(station);out.source='BursaKart / BURULAŞ';
 }else if(cities.find(c=>c.id===city)?.provider==='asis'){
  const c=cities.find(c=>c.id===city),l=local;if(!l)throw Error('ASİS hattı bulunamadı');const d=await asis(c,'Avl/Get/RouteByRouteId?RouteIds='+encodeURIComponent(l.id));const v=d.Result?.Route?.[0]?.RouteVariants?.[direction==='D'?1:0]||d.Result?.Route?.[0]?.RouteVariants?.[0];if(!v)throw Error('Bu hat için servis güzergâh yayımlamadı');const g=typeof v.Geom==='string'?JSON.parse(v.Geom):v.Geom;const geom=g.geometry||g;const parts=geom.type==='LineString'?[geom.coordinates]:geom.coordinates;out.parts=parts.map(a=>a.map(p=>({lat:p[1],lon:p[0]})).filter(location));const s=await asis(c,'Avl/Get/RouteVariantStations?RouteVariantId='+v.Id);out.stops=(s.Result?.routeVariantStops?.[0]?.station||[]).map(station);out.source=c.name+' / ASİS';
 }else{
  if(!local)throw Error('Hat katalogda bulunamadı');const byId=new Map(data.stops.map(s=>[s.id,s]));out.stops=(data.routeStops?.[local.id]||[]).map(id=>byId.get(id)).filter(Boolean);
  if(city===0){const s=await schedule(local.code);const byCode=new Map(data.stops.map(x=>[x.code,x]));out.stops=s.stops.map(st=>({...st,lat:byCode.get(st.code)?.lat,lon:byCode.get(st.code)?.lon}));out.schematic=true;out.source=s.source;}
  if(city===1){try{const url='https://otobusum.metkm.win/v1/';const [paths,stops]=await Promise.all([request(url+'routes/'+encodeURIComponent(code)+'?city=istanbul'),request(url+'route-stops/'+encodeURIComponent(code)+'?direction='+direction+'&city=istanbul')]);const want=variant||data.external?.[local.id],p=paths.find(p=>p.code===want)||paths.find(p=>p.code?.includes('_'+direction+'_'))||paths[0];if(p?.path?.length)out.parts=[p.path.map(station).filter(location)];out.stops=stops.map(r=>({code:r.code,name:r.name,lat:Number(r.lat),lon:Number(r.lng)}));out.source='İETT / Otobüsüm Anlık';}catch{out.notice='Çevrimiçi yol geometrisi alınamadı; yerel durak bağlantısı.';}}
  if(!out.parts.length)try{const shape=JSON.parse(gunzipSync(readFileSync(path.join(DATA,'route-shapes',local.id+'.dbz'))));out.parts=[shape.map(p=>({lat:p[0],lon:p[1]}))];out.source='Yerel kaynak güzergâhı';}catch{}
  if(!out.parts.length){out.parts=[out.stops.filter(location)];out.schematic=true;out.notice+=(out.notice?' ':'')+'Bilinen durakların şematik bağlantısı; gerçek yol geometrisi değildir.';}
 }
 if(out.parts.reduce((n,p)=>n+p.length,0)>20000)throw Error('Rota nokta sınırı aşıldı');if(!out.parts.some(p=>p.length>1))throw Error('Çizim için yeterli konum bulunamadı');return out;
}
export async function rail(city,id,instant=Date.now()){
 const lines=railData.lines.filter(l=>l.city===city);const selected=lines.find(l=>l.id===id),result={lines,trains:[],explanation:'Tahmini trenleri görmek için bir hat seç. GPS değildir.'};if(!selected)return result;
 const modelId=city===0&&['M1','M2','M3'].includes(id)?'M123':id;const profile=structuredClone(railModel.profiles.find(p=>p.city===city&&p.id===modelId));if(!profile){result.explanation='Bu hat için saat modeli yok; istasyon ağı gösteriliyor.';return result;}
 if(city===0){const codes={M123:['M1-D','M1-G'],M4:['M4-G','M4-D'],A1:['A1-D','A1-G']}[modelId];if(codes){try{const values=await Promise.all(codes.map(c=>schedule(c)));if(values.every(v=>v.duration_minutes>0)){profile.tripSeconds=values.map(v=>v.duration_minutes*60);profile.departures=values.map(v=>v.days.map(day=>[...new Set(day.filter(t=>/^\d\d:\d\d$/.test(t)).map(t=>{const sec=+t.slice(0,2)*3600+ +t.slice(3)*60;return sec<21600?sec+86400:sec;}))].sort((a,b)=>a-b)));}}catch{}}}
 let stations=selected.stations;if(modelId==='M123'){const get=id=>lines.find(l=>l.id===id)?.stations||[];stations=[...get('M2').slice().reverse(),...get('M1').slice(1),...get('M3').slice(1)];}
 const now=new Date(instant+10800000);if(now.getUTCHours()<6)now.setUTCDate(now.getUTCDate()-1);now.setUTCHours(0,0,0,0);const start=+now-10800000,seconds=(instant-start)/1000,weekday=now.getUTCDay(),day=weekday===6?1:weekday===0?2:0;
 const scheduled=profile.kind==='departures',dwell=railModel.dwellSeconds||20;result.explanation=(scheduled?'EGO kalkış çizelgesine dayalı':'Sefer sıklığı varsayımına dayalı')+' tahmin · GPS değil. '+railModel.assumptions;result.source=profile.sources?.[0];
 const dist=(a,b)=>Math.max(.00001,Math.hypot((a.lon-b.lon)*Math.cos((a.lat+b.lat)*Math.PI/360),a.lat-b.lat));const total=stations.slice(1).reduce((sum,b,i)=>sum+dist(stations[i],b),0),allowed=new Set(selected.stations.map(s=>s.name));
 for(let direction=0;direction<2;direction++){
  const duration=profile.tripSeconds[direction];let times=scheduled?profile.departures[direction][day]:Array.from({length:Math.floor((profile.endSeconds-profile.startSeconds)/profile.headwaySeconds)+1},(_,i)=>profile.startSeconds+i*profile.headwaySeconds);if(!scheduled&&profile.nightWeekend&&[5,6].includes(weekday))times=[...times,...Array.from({length:12},(_,i)=>86400+i*1800)];
  for(const departure of times){const elapsed=seconds-departure;if(elapsed<0||elapsed>=duration)continue;const travel=Math.max(duration*.5,duration-dwell*(stations.length-2)),wait=(duration-travel)/Math.max(1,stations.length-2);let clock=0;for(let step=0;step<stations.length-1;step++){const from=direction===0?step:stations.length-1-step,to=direction===0?step+1:stations.length-2-step,a=stations[from],b=stations[to],leg=travel*dist(a,b)/total;if(elapsed<clock+leg+(step<stations.length-2?wait:0)){if(modelId!=='M123'||allowed.has(a.name)&&allowed.has(b.name)){const f=Math.max(0,Math.min(1,(elapsed-clock)/leg));result.trains.push({line:id,name:a.name+' → '+b.name,lat:a.lat+(b.lat-a.lat)*f,lon:a.lon+(b.lon-a.lon)*f,eta:'≈ '+Math.ceil(Math.max(0,clock+leg-elapsed)/60)+' dk',estimated:true,direction:direction?'D':'G'});}break;}clock+=leg+(step<stations.length-2?wait:0);}if(result.trains.length>=128)return result;
  }
 }
 return result;
}
