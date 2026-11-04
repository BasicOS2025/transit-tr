import { chromium } from 'playwright';
export const EGO_PAGE='https://www.ego.gov.tr/otobusnerede';
export function parseEta(text){const match=/^(?:(\d+)\s*dk)?\s*(?:(\d+)\s*sn)?$/i.exec(text.trim());if(!match||(!match[1]&&!match[2]))return null;return Number(match[1]||0)*60+Number(match[2]||0);}
export class EgoBrowser {
 constructor(options={}){this.browser=null;this.launchOptions=options.launchOptions||{};this.pageURL=options.pageURL||EGO_PAGE;this.timeout=options.timeout||25000;}
 async query(stop){if(!/^\d{5}$/.test(stop))throw Error('invalid_stop');if(!this.browser||!this.browser.isConnected())this.browser=await chromium.launch({headless:true,...this.launchOptions});const context=await this.browser.newContext();const page=await context.newPage();const timer=setTimeout(()=>context.close().catch(()=>{}),this.timeout);page.setDefaultTimeout(this.timeout);page.setDefaultNavigationTimeout(this.timeout);try{
 await page.goto(this.pageURL,{waitUntil:'domcontentloaded'});
 const form=page.locator('form.bus-form');const action=new URL(await form.getAttribute('action'),this.pageURL);if(action.origin!==new URL(this.pageURL).origin||!/^\/otobusnerede\/[a-zA-Z0-9_-]{1,48}$/.test(action.pathname)||action.search||action.hash)throw Error('source_changed');await form.locator('input[type="text"]').fill(stop);
 await Promise.all([page.waitForURL(u=>u.origin===action.origin&&u.pathname===action.pathname,{waitUntil:'domcontentloaded'}),form.getByRole('button',{name:'Otobüs Nerede?',exact:true}).click()]);
 const result=await page.locator('.bus-list').evaluate(list=>({cards:Array.from(list.querySelectorAll('.bus-card')).slice(0,40).map(card=>({route:card.querySelector('.route-badge')?.textContent.trim()||'',destination:card.querySelector('.route-title')?.textContent.trim()||'',etaText:card.querySelector('.eta-mins')?.textContent.trim()||''})),sourceTime:Array.from(list.querySelectorAll('center')).map(e=>e.textContent.trim()).find(s=>/^\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}$/.test(s))||null}));
 if(!result.sourceTime)throw Error('source_changed');const arrivals=result.cards.map(row=>({...row,etaSeconds:parseEta(row.etaText)}));if(arrivals.some(row=>!row.route||!row.destination||row.etaSeconds===null))throw Error('source_changed');
 return {stop,arrivals,source:'EGO Otobüs Nerede',sourceURL:EGO_PAGE,sourceTime:result.sourceTime,checkedAt:new Date().toISOString(),kind:'website_estimated_arrivals',vehiclePositions:false,cache:false};
 }finally{clearTimeout(timer);await context.close();}}
 async close(){if(this.browser){await this.browser.close();this.browser=null;}}
}
