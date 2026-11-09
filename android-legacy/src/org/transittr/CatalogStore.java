package org.transittr;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;

/** Read-only local catalogue. All methods are called on the catalogue's single worker. */
public final class CatalogStore {
 public static final class Item {
  public int id,kind,source;public String code,name,type;public double lat=Double.NaN,lon=Double.NaN,distance=-1;
  public boolean hasPosition(){return lat>=35&&lat<=43&&lon>=25&&lon<=45;}
 }
 public static final class Page {public final ArrayList<Item> items=new ArrayList<Item>();public int total,routes,stops;}
 private final Context context;private final SharedPreferences favorites;private SQLiteDatabase db;
 public CatalogStore(Context context){this.context=context.getApplicationContext();favorites=context.getSharedPreferences("MainActivity",0);}
 private void open()throws IOException{
  if(db!=null)return;StorageBudget.prepare(context);File target=context.getDatabasePath("transit-v120.sqlite");
  synchronized(CatalogStore.class){if(!target.exists()){target.getParentFile().mkdirs();File tmp=new File(target+".catalog.tmp");InputStream in=new java.util.zip.GZIPInputStream(context.getAssets().open("transit-v101.sqlite.dbz"),8192);FileOutputStream out=new FileOutputStream(tmp);try{byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>8388608)throw new IOException("Katalog boyutu sınırı");out.write(b,0,n);}}finally{in.close();out.close();}if(!tmp.renameTo(target)){tmp.delete();if(!target.exists())throw new IOException("Katalog hazırlanamadı");}}
  }
  for(String obsolete:new String[]{"transit-v09.sqlite","transit-v101.sqlite"}){File old=context.getDatabasePath(obsolete);if(old.exists())old.delete();}
  db=SQLiteDatabase.openDatabase(target.toString(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
 }
 public static String searchSql(){String value="code||' '||name";String from="İıŞşĞğÜüÖöÇçÂâÎîÛû",to="iissgguuooccaaiiuu";for(int i=0;i<from.length();i++)value="replace("+value+",'"+from.charAt(i)+"','"+to.charAt(i)+"')";return "lower("+value+")";}
 public static String normalize(String value){return value.toLowerCase(new Locale("tr","TR")).replace('ı','i').replace('ş','s').replace('ğ','g').replace('ü','u').replace('ö','o').replace('ç','c').replace('â','a').replace('î','i').replace('û','u');}
 private String key(Item item){return "fav:"+item.kind+":"+item.id;}
 public boolean saved(Item item){return favorites.getBoolean(key(item),false);}
 public void toggle(Item item){NetworkCatalog.remember(context,item);favorites.edit().putBoolean(key(item),!saved(item)).commit();}
 private String savedIds(int kind){StringBuilder ids=new StringBuilder();String prefix="fav:"+kind+":";for(Map.Entry<String,?> entry:favorites.getAll().entrySet())if(entry.getKey().startsWith(prefix)&&Boolean.TRUE.equals(entry.getValue()))try{int id=Integer.parseInt(entry.getKey().substring(prefix.length()));if(id>0){if(ids.length()>0)ids.append(',');ids.append(id);}}catch(NumberFormatException ignored){}return ids.length()==0?"-1":ids.toString();}
 private int count(String table,int city)throws IOException{open();Cursor c=db.rawQuery("SELECT count(*) FROM "+table+" WHERE city=?",new String[]{""+city});try{c.moveToFirst();return c.getInt(0);}finally{c.close();}}
 private static Item item(Cursor c){Item row=new Item();row.id=c.getInt(0);row.kind=c.getInt(1);row.code=c.getString(2);row.name=c.getString(3);row.type=c.getString(4);if(!c.isNull(5))row.lat=c.getDouble(5);if(!c.isNull(6))row.lon=c.getDouble(6);row.source=c.getInt(7);return row;}
 public Page page(int city,int mode,String query,int offset)throws IOException{
  if(NetworkCatalog.city(city))return NetworkCatalog.page(context,city,mode,query,offset);
  open();Page page=new Page();page.routes=count("routes",city);page.stops=count("stops",city);
  String match="%"+normalize(query).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";
  String expression=query.trim().matches("[0-9-]*")?"(code||' '||name)":searchSql();
  String routes="SELECT id,0 kind,code,name,type,NULL lat,NULL lon,source FROM routes WHERE city=? AND "+expression+" LIKE ? ESCAPE '\\'";
  String stops="SELECT id,1 kind,code,name,'Durak' type,lat,lon,source FROM stops WHERE city=? AND "+expression+" LIKE ? ESCAPE '\\'";
  String sql;String[] args;
  if(mode==2){sql=routes+" AND id IN ("+savedIds(0)+") UNION ALL "+stops+" AND id IN ("+savedIds(1)+")";args=new String[]{""+city,match,""+city,match};}
  else{sql=mode==0?routes:stops;args=new String[]{""+city,match};}
  Cursor count=db.rawQuery("SELECT count(*) FROM ("+sql+")",args);try{count.moveToFirst();page.total=count.getInt(0);}finally{count.close();}
  Cursor rows=db.rawQuery("SELECT * FROM ("+sql+") ORDER BY CASE WHEN lower(code)=? THEN 0 ELSE 1 END,kind,code,name LIMIT 50 OFFSET "+Math.max(0,offset),append(args,normalize(query)));
  try{while(rows.moveToNext())page.items.add(item(rows));}finally{rows.close();}return page;
 }
 private static String[] append(String[] values,String last){String[] result=new String[values.length+1];System.arraycopy(values,0,result,0,values.length);result[values.length]=last;return result;}
 public ArrayList<Item> routeStops(int route)throws IOException{
  open();ArrayList<Item> result=new ArrayList<Item>();Cursor c=db.rawQuery("SELECT s.id,1,s.code,s.name,'Durak',s.lat,s.lon,s.source FROM route_stops rs JOIN stops s ON s.id=rs.stop WHERE rs.route=? ORDER BY rs.seq",new String[]{""+route});try{while(c.moveToNext())result.add(item(c));}finally{c.close();}return result;
 }
 public String source(int id)throws IOException{open();Cursor c=db.rawQuery("SELECT name,url,license,end FROM sources WHERE id=?",new String[]{""+id});try{if(!c.moveToFirst())return "";return c.getString(0)+"\n"+c.getString(1)+"\n"+c.getString(2)+(c.getString(3).length()>0?"\nTakvim bitişi: "+c.getString(3):"")+"\nYerel veri anlık hizmet durumunu göstermez.";}finally{c.close();}}
 private static class Departure {int delta;String label;Departure(int d,String s){delta=d;label=s;}}
 public String schedule(int stop)throws IOException{
  open();org.json.JSONArray rows=CompactData.read(context,"stop-times",stop);Calendar now=Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00"));int seconds=now.get(Calendar.HOUR_OF_DAY)*3600+now.get(Calendar.MINUTE)*60+now.get(Calendar.SECOND);ArrayList<Departure> times=new ArrayList<Departure>();HashMap<String,Boolean> enabled=new HashMap<String,Boolean>();HashMap<Integer,String> labels=new HashMap<Integer,String>();
  try{for(int offset=-1;offset<=1;offset++){Calendar day=(Calendar)now.clone();day.add(Calendar.DATE,offset);String date=String.format(Locale.US,"%04d%02d%02d",day.get(Calendar.YEAR),day.get(Calendar.MONTH)+1,day.get(Calendar.DAY_OF_MONTH));int mask=1<<((day.get(Calendar.DAY_OF_WEEK)+5)%7),low=seconds-offset*86400;
   for(int i=0;i<rows.length();i++){org.json.JSONArray row=rows.getJSONArray(i);int route=row.getInt(0),t=row.getInt(2);String service=row.getString(1),key=service+":"+date;if(t<low||t>=low+86400)continue;Boolean active=enabled.get(key);if(active==null){Cursor c=db.rawQuery("SELECT s.start,s.end,s.mask,e.type FROM services s LEFT JOIN exceptions e ON e.service=s.id AND e.date=? WHERE s.id=?",new String[]{date,service});try{boolean yes=false;if(c.moveToFirst())yes=!c.isNull(3)?c.getInt(3)==1:c.getString(0).compareTo(date)<=0&&c.getString(1).compareTo(date)>=0&&(c.getInt(2)&mask)!=0;active=Boolean.valueOf(yes);enabled.put(key,active);}finally{c.close();}}if(!active.booleanValue())continue;String label=labels.get(Integer.valueOf(route));if(label==null){Cursor c=db.rawQuery("SELECT code,name FROM routes WHERE id=?",new String[]{""+route});try{label=c.moveToFirst()?c.getString(0)+" · "+c.getString(1):"";labels.put(Integer.valueOf(route),label);}finally{c.close();}}times.add(new Departure(t-low,String.format(Locale.US,"%02d:%02d",(t/3600)%24,(t/60)%60)+" · "+label));}
  }}catch(org.json.JSONException e){throw new IOException("Tarife okunamadı");}
  Collections.sort(times,new Comparator<Departure>(){public int compare(Departure a,Departure b){return a.delta-b.delta;}});StringBuilder text=new StringBuilder("Planlı kalkışlar · Türkiye saati\nCanlı varış değildir.\n");for(int i=0;i<times.size()&&i<8;i++)text.append(times.get(i).label).append('\n');if(times.isEmpty())text.append("Önümüzdeki 24 saat için geçerli tarife kaydı yok.");return text.toString();
 }
 public Page nearby(int city,double lat,double lon)throws IOException{
  if(NetworkCatalog.city(city))return NetworkCatalog.nearby(context,city,lat,lon);
  open();if(Double.isNaN(lat)||Double.isNaN(lon)||Math.abs(lat)>90||Math.abs(lon)>180)throw new IOException("Geçerli koordinat gir");Page result=new Page();result.routes=count("routes",city);result.stops=count("stops",city);double width=.025/Math.max(.05,Math.cos(lat*Math.PI/180));
  Cursor c=db.rawQuery("SELECT id,1,code,name,'Durak',lat,lon,source FROM stops WHERE city=? AND lat BETWEEN ? AND ? AND lon BETWEEN ? AND ?",new String[]{""+city,""+(lat-.02),""+(lat+.02),""+(lon-width),""+(lon+width)});
  try{while(c.moveToNext()){Item row=item(c);row.distance=distance(lat,lon,row.lat,row.lon);if(row.distance<=2000)result.items.add(row);}}finally{c.close();}
  Collections.sort(result.items,new Comparator<Item>(){public int compare(Item a,Item b){return Double.compare(a.distance,b.distance);}});result.total=result.items.size();while(result.items.size()>50)result.items.remove(result.items.size()-1);return result;
 }
 private static double distance(double a,double b,double c,double d){double r=Math.PI/180,x=Math.sin((c-a)*r/2),y=Math.sin((d-b)*r/2),h=x*x+Math.cos(a*r)*Math.cos(c*r)*y*y;return 12742000*Math.asin(Math.sqrt(Math.min(1,h)));}
 public String external(int route)throws IOException{open();Cursor c=db.rawQuery("SELECT code FROM route_external WHERE route=?",new String[]{""+route});try{return c.moveToFirst()?c.getString(0):"";}finally{c.close();}}
 public ArrayList<JourneyData.Station> shape(int route)throws IOException{return CompactData.shape(context,route);}
 public Item stop(int city,String code)throws IOException{
  if(NetworkCatalog.city(city))return NetworkCatalog.stop(context,city,code);open();Cursor c=db.rawQuery("SELECT id,1,code,name,'Durak',lat,lon,source FROM stops WHERE city=? AND code=? LIMIT 1",new String[]{""+city,code});try{return c.moveToFirst()?item(c):null;}finally{c.close();}}
 public Item uniqueKonyaStop(String code)throws IOException{open();Cursor c=db.rawQuery("SELECT id,1,code,name,'Durak',lat,lon,source FROM stops WHERE city=6 AND code GLOB ? LIMIT 2",new String[]{code+"[a-zA-Z]"});try{if(!c.moveToFirst())return null;Item found=item(c);return c.moveToNext()?null:found;}finally{c.close();}}
 public void close(){if(db!=null){db.close();db=null;}}
}
