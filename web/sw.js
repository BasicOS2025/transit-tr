const CACHE='transit-tr-v9-ankara-catalog';
const ASSETS=['./','./index.html','./app.js','./style.css','./icons/star.svg','./icons/star-fill.svg','./fonts/Geist-Regular.ttf','./core.js','./manifest.webmanifest','./icon-192.png','./icon-512.png',...Array.from({length:7},(_,i)=>'./data/'+i+'.json')];
self.addEventListener('install',e=>e.waitUntil(caches.open(CACHE).then(c=>c.addAll(ASSETS))));
self.addEventListener('activate',e=>e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('transit-tr-')&&k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',e=>{const u=new URL(e.request.url);if(e.request.method!=='GET'||u.origin!==location.origin||!ASSETS.some(a=>new URL(a,self.registration.scope).href===u.href))return;e.respondWith(caches.match(e.request).then(r=>r||fetch(e.request)));});
