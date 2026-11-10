const KEY='transit:legal-first-run-v1';
export function noticeSeen(storage){try{return storage?.getItem(KEY)==='true';}catch{return false;}}
export function rememberNotice(storage){try{storage?.setItem(KEY,'true');}catch{/* Private/blocked storage: show again next visit. */}}
export async function showFirstRunNotice(){
 let storage;try{storage=globalThis.localStorage;}catch{}
 if(noticeSeen(storage))return;
 return new Promise(resolve=>{
  const dialog=document.createElement('dialog');dialog.setAttribute('aria-labelledby','first-run-title');dialog.style.cssText='width:min(680px,92vw);max-height:88vh;padding:20px;';
  const title=document.createElement('h2');title.id='first-run-title';title.textContent='Gizlilik ve sorumluluk bildirimi';dialog.append(title);
  const intro=document.createElement('p');intro.textContent='Lütfen bildirimi incele. Bu bilgilendirme açık rıza veya kanuni haklardan feragat değildir.';dialog.append(intro);
  const content=document.createElement('div');content.style.cssText='max-height:55vh;overflow:auto;white-space:pre-wrap;';content.textContent='Bildirim yükleniyor…';dialog.append(content);
  const controls=document.createElement('div');controls.style.cssText='display:flex;gap:12px;flex-wrap:wrap;margin-top:16px;';dialog.append(controls);
  const exit=document.createElement('button');exit.textContent='Şimdi değil';controls.append(exit);
  const retry=document.createElement('button');retry.textContent='Yeniden yükle';retry.hidden=true;controls.append(retry);
  const next=document.createElement('button');next.textContent='Okudum, devam et';next.className='primary';next.disabled=true;controls.append(next);
  next.onclick=()=>{rememberNotice(storage);dialog.close();dialog.remove();resolve();};
  exit.onclick=()=>{content.textContent='Uygulama açılmadı. Bu sekmeyi kapatabilir veya devam etmek için sayfayı yeniden yükleyebilirsin.';controls.replaceChildren();};
  dialog.addEventListener('cancel',event=>event.preventDefault());
  document.body.append(dialog);dialog.showModal();
  async function load(){retry.hidden=true;next.disabled=true;try{const texts=await Promise.all(['privacy.txt','terms.txt'].map(async name=>{const response=await fetch(name);if(!response.ok)throw Error('Bildirim alınamadı');return response.text();}));content.textContent=texts.join('\n\n');next.disabled=false;}catch{content.textContent='Bildirim yüklenemedi. İnternet/site bağlantısını kontrol edip yeniden yükle.';retry.hidden=false;}}
  retry.onclick=load;load();
 });
}
