import http from 'node:http';
import {readFile} from 'node:fs/promises';
import path from 'node:path';
import {gzipSync} from 'node:zlib';
import {fileURLToPath} from 'node:url';
import {cities,live,request,catalog} from './transport.mjs';
import {catalogPage,nearby,route,rail,schedule,syncJob,startSync,plannedDepartures} from './journeys.mjs';
const WEB=path.resolve(fileURLToPath(new URL('../web/',import.meta.url)));
const mime={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.json':'application/json; charset=utf-8','.webmanifest':'application/manifest+json','.svg':'image/svg+xml','.png':'image/png','.ttf':'font/ttf','.woff2':'font/woff2','.txt':'text/plain; charset=utf-8'};
const cache=new Map(),pending=new Map();let active=0;
const expireCache=setInterval(()=>{for(const [key,entry] of cache)if(entry.expires<=Date.now())cache.delete(key);},60000);expireCache.unref();
async function cached(key,ttl,load){const old=cache.get(key);if(old&&Date.now()-old.at<ttl)return old.value;if(pending.has(key))return pending.get(key);if(active>=8)throw Object.assign(Error('Servis meşgul; biraz sonra yeniden dene.'),{status:429});active++;const promise=load().then(value=>{cache.set(key,{value,at:Date.now(),expires:Date.now()+ttl});while(cache.size>64)cache.delete(cache.keys().next().value);return value;}).finally(()=>{active--;pending.delete(key);});pending.set(key,promise);return promise;}
const send=(res,status,data)=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(JSON.stringify(data));};
export const server=http.createServer(async(req,res)=>{try{
 const u=new URL(req.url,'http://localhost'),q=u.searchParams,p=u.pathname;
 if(p.startsWith('/api/')){
  if(!['GET','POST'].includes(req.method))return send(res,405,{message:'Yöntem desteklenmiyor'});
  if(p==='/api/v2/health')return send(res,200,{status:'ok',version:'1.4.0',cities:cities.length});
  if(p==='/api/v2/cities')return send(res,200,cities.map(({key,error,...c})=>c));
  if(p==='/api/v2/sync'){if(req.method==='POST'){if(req.headers.origin&&new URL(req.headers.origin).host!==req.headers.host)return send(res,403,{message:'Kaynak eşleşmiyor'});if(q.get('cancel')==='1')syncJob.cancelled=true;else startSync();}return send(res,200,syncJob);}
  if(req.method!=='GET')return send(res,405,{message:'Bu sorgu GET gerektirir'});
  if(p==='/api/v2/statuses'){return send(res,200,await cached(p,60000,()=>request('https://api.ibb.gov.tr/MetroIstanbul/api/MetroMobile/V2/GetServiceStatuses')));}
  const city=Number(q.get('city')||0);if(!cities.some(c=>c.id===city))return send(res,400,{message:'Geçersiz şehir'});
  const line=q.get('line')||'',stop=q.get('stop')||'',direction=q.get('direction')||'G';
  if(!/^[A-Za-z0-9ÇĞİÖŞÜçğıöşü_ -]{0,80}$/.test(line)||!['G','D',''].includes(direction))return send(res,400,{message:'Hat veya yön geçersiz'});
  let result;
  if(p==='/api/v2/offline'){const data=gzipSync(Buffer.from(JSON.stringify(catalog(city))));res.writeHead(200,{'Content-Type':'application/octet-stream','Cache-Control':'no-cache','X-Content-Type-Options':'nosniff'});return res.end(data);}
  if(p==='/api/v2/live'){if(!/^\d{1,8}$/.test(stop)||city===0&&stop.length!==5)return send(res,400,{message:'Geçerli durak numarası gir'});result=await cached(u.search,city===1?45000:15000,()=>live(city,stop,line,q.get('direction')||''));}
  else if(p==='/api/v2/catalog'){const term=q.get('q')||'';if(term.length>100)return send(res,400,{message:'Arama çok uzun'});const offset=Number(q.get('offset')||0);if(!Number.isInteger(offset)||offset<0||offset>100000)return send(res,400,{message:'Geçersiz sayfa'});const kind=q.get('kind')||'routes';if(!['routes','stops'].includes(kind))return send(res,400,{message:'Geçersiz katalog türü'});result=await cached(p+u.search,60000,()=>catalogPage(city,kind,term,offset));}
  else if(p==='/api/v2/nearby'){const a=Number(q.get('lat')),b=Number(q.get('lon'));if(!q.has('lat')||!q.has('lon')||!Number.isFinite(a)||!Number.isFinite(b)||Math.abs(a)>=85||Math.abs(b)>=180)return send(res,400,{message:'Geçersiz koordinat'});result=await cached(p+u.search,60000,()=>nearby(city,a,b));}
  else if(p==='/api/v2/route'){if(!line||!/^[A-Za-z0-9ÇĞİÖŞÜçğıöşü_-]{1,32}$/.test(line))return send(res,400,{message:'Geçersiz hat'});const variant=q.get('variant')||'';if(variant.length>80)return send(res,400,{message:'Geçersiz varyant'});result=await cached(p+u.search,3600000,()=>route(city,line,variant,direction));}
  else if(p==='/api/v2/rail')result=await rail(city,line);
  else if(p==='/api/v2/stop-times'){if(!/^\d{1,8}[a-zA-Z]?$/.test(stop))return send(res,400,{message:'Geçersiz durak'});result=plannedDepartures(city,stop);}
  else if(p==='/api/v2/schedule')result=await cached(p+line+(q.get('refresh')||''),q.get('refresh')==='1'?30000:60000,()=>schedule(line,q.get('refresh')==='1'));
  else return send(res,404,{message:'Servis bulunamadı'});
  return send(res,200,result);
 }
 if(!['GET','HEAD'].includes(req.method)){res.writeHead(405);return res.end();}
 let name=decodeURIComponent(p);if(name==='/')name='/index.html';const file=path.resolve(WEB,'.'+name);if(!file.startsWith(WEB+path.sep)){res.writeHead(403);return res.end();}let data;try{data=await readFile(file);}catch{res.writeHead(404);return res.end('Bulunamadı');}
 res.writeHead(200,{'Content-Type':mime[path.extname(file)]||'application/octet-stream','X-Content-Type-Options':'nosniff','Cache-Control':/\.(ttf|png|woff2)$/.test(file)?'public, max-age=604800':'no-cache','Referrer-Policy':'strict-origin-when-cross-origin'});res.end(req.method==='HEAD'?undefined:data);
 }catch(e){send(res,e.status||502,{message:e.message||'Ulaşım verisi alınamadı'});}});
server.requestTimeout=45000;server.headersTimeout=15000;server.maxHeadersCount=40;
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url))server.listen(Number(process.env.PORT||8080),process.env.HOST||'127.0.0.1',()=>console.log('Transit TR http://'+(process.env.HOST||'127.0.0.1')+':'+(process.env.PORT||8080)));
