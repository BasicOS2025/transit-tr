import test from 'node:test';import assert from 'node:assert/strict';import {server} from '../server.mjs';
test('Standalone HTTP service serves UI, protects internal configuration and validates requests',async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));const base='http://127.0.0.1:'+server.address().port;try{
 assert.equal((await fetch(base+'/')).status,200);assert.match(await(await fetch(base+'/live-app.js')).text(),/TransitMap/);
 const cityResponse=await fetch(base+'/api/v2/cities');const data=await cityResponse.json();assert.equal(data.length,39);assert.ok(data.every(c=>!Object.hasOwn(c,'key')));
 assert.ok(data.every(c=>c.status!=='unavailable'));const rail=await(await fetch(base+'/api/v2/rail?city=2&line=%C4%B0ZBAN')).json();assert.equal(rail.lines[0].stations.length,41);assert.equal((await fetch(base+'/api/v2/izban-times?from=1&to=1')).status,400);
 assert.equal((await fetch(base+'/api/v2/catalog?city=999999')).status,400);
 assert.equal((await fetch(base+'/api/v2/live?city=0&stop=10940&line='+encodeURIComponent('../secret'))).status,400);
 assert.equal((await fetch(base+'/api/v2/nearby?city=0&lat=NaN&lon=32')).status,400);
 assert.equal((await fetch(base+'/api/v2/sync',{method:'POST',headers:{Origin:'https://unrelated.example'}})).status,403);
 assert.equal((await fetch(base+'/api/v2/live',{method:'PUT'})).status,405);
 assert.equal((await fetch(base+'/web-service/data/bursa-bootstrap.json')).status,404);
}finally{await new Promise(r=>server.close(r));}});
