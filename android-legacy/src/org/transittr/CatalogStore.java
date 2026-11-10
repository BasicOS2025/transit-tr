package org.transittr;

import android.content.*;


import java.io.*;
import java.util.*;

/** Read-only local catalogue. All methods are called on the catalogue's single worker. */
public final class CatalogStore {
 public static final class Item {
  public int id,kind,source;public String code,name,type;public double lat=Double.NaN,lon=Double.NaN,distance=-1;
  public boolean hasPosition(){return lat>=35&&lat<=43&&lon>=25&&lon<=45;}
 }
 public static final class Page {public final ArrayList<Item> items=new ArrayList<Item>();public int total,routes,stops;}
 private final Context context;private final SharedPreferences favorites;private final StreamCatalog stream;
 public CatalogStore(Context c){context=c.getApplicationContext();favorites=c.getSharedPreferences("MainActivity",0);stream=new StreamCatalog(c);}
 public static String searchSql(){String value="code||' '||name";String from="İıŞşĞğÜüÖöÇçÂâÎîÛû",to="iissgguuooccaaiiuu";for(int i=0;i<from.length();i++)value="replace("+value+",'"+from.charAt(i)+"','"+to.charAt(i)+"')";return "lower("+value+")";}
 public static String normalize(String value){return value.toLowerCase(new Locale("tr","TR")).replace('ı','i').replace('ş','s').replace('ğ','g').replace('ü','u').replace('ö','o').replace('ç','c').replace('â','a').replace('î','i').replace('û','u');}
 public boolean saved(Item i){return favorites.getBoolean("fav:"+i.kind+":"+i.id,false);}
 public void toggle(Item i){boolean was=saved(i);if(!was){int count=0;for(java.util.Map.Entry<String,?> e:favorites.getAll().entrySet())if(e.getKey().startsWith("fav:")&&Boolean.TRUE.equals(e.getValue()))count++;if(count>=200){android.widget.Toast.makeText(context,"En fazla 200 katalog kaydı saklanabilir",android.widget.Toast.LENGTH_SHORT).show();return;}NetworkCatalog.remember(context,i);}SharedPreferences.Editor edit=favorites.edit();if(was){edit.remove("fav:"+i.kind+":"+i.id);edit.remove("network-item:"+i.kind+":"+i.id);}else edit.putBoolean("fav:"+i.kind+":"+i.id,true);edit.commit();}
 public Page page(int city,int mode,String q,int offset)throws IOException{return NetworkCatalog.city(city)?NetworkCatalog.page(context,city,mode,q,offset):stream.page(city,mode,q,offset);}
 public ArrayList<Item> routeStops(int id)throws IOException{return stream.routeStops(id);}
 public HashMap<String,Item> stops(int city,HashSet<String> codes)throws IOException{return stream.stops(city,codes);}
 public String source(int id)throws IOException{return stream.source(id);}
 public String schedule(int id)throws IOException{return stream.schedule(id);}
 public Page nearby(int city,double a,double b)throws IOException{return NetworkCatalog.city(city)?NetworkCatalog.nearby(context,city,a,b):stream.nearby(city,a,b);}
 public String external(int id)throws IOException{return stream.external(id);}
 public ArrayList<JourneyData.Station> shape(int id)throws IOException{return CompactData.shape(context,id);}
 public Item stop(int city,String code)throws IOException{return NetworkCatalog.city(city)?NetworkCatalog.stop(context,city,code):stream.stop(city,code,false);}
 public Item uniqueKonyaStop(String code)throws IOException{return stream.stop(6,code,true);}
 public void close(){}
}
