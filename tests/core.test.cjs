const assert=require('node:assert/strict'),core=require('../web/core.js');
assert.equal(core.normalize('İSTANBUL IĞDIR ŞİŞLİ'),'istanbul igdir sisli');
assert.equal(core.distance(0,0,0,0),0);assert.ok(Math.abs(core.distance(0,0,0,1)-111195)<2);assert.throws(()=>core.nearby([],NaN,1));assert.throws(()=>core.nearby([],91,0));
assert.equal(core.nearby([{id:1,lat:41,lon:29},{id:2,lat:41.001,lon:29},{id:3,lat:42,lon:29}],41.001,29)[0].stop.id,2);
const c={routes:[{id:1,code:'M1',name:'Test'}],services:[{id:'s',start:'20260101',end:'20261231',mask:127,exceptions:{}}],departures:[[1,1,'s',25*3600],[1,1,'s',2*3600]]};
let results=core.schedule(c,1,new Date('2026-11-03T21:30:00Z'));assert.equal(results[0].time,'01:00');assert.equal(results[0].date,'2026-11-04');assert.equal(results[0].delta,1800);assert.equal(results[1].time,'02:00');
c.services[0].exceptions['20261103']=2;assert.equal(core.schedule(c,1,new Date('2026-11-03T21:30:00Z'))[0].time,'02:00');c.services[0].end='20250101';assert.equal(core.schedule(c,1,new Date('2026-11-03T21:30:00Z')).length,0);c.services[0].exceptions['20261104']=1;assert.ok(core.schedule(c,1,new Date('2026-11-03T21:30:00Z')).length>0);
assert.match(core.answer('CANLI METRO NE ZAMAN'),/bağlı değil/);assert.match(core.answer('Kart BAKİYESİ'),/bağlı değil/);assert.match(core.answer('YAKINDAKİ DURAK'),/GPS/);assert.match(core.answer(''),/Bir soru/);
console.log('PASS: Turkish normalization, distance, invalid coordinates, nearby order/radius, UTC+3 midnight, GTFS >24h, canceled/added services, expired calendar and offline assistant');

assert.deepEqual(core.nearby([{id:9,lat:null,lon:null}],0,0),[]);
