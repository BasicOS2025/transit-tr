package org.transittr;

import android.content.Context;
import java.io.*;import java.net.URLEncoder;import java.util.*;import org.json.*;

/** One selected line at a time. Keeps true geometry separate from schematic stop links. */
public final class RouteRepository {
 private static JourneyData.Station station(CatalogStore.Item item){JourneyData.Station s=new JourneyData.Station(item.name,item.lat,item.lon);s.code=item.code;return s;}
 private static boolean located(JourneyData.Station s){return s.lat>=35&&s.lat<=43&&s.lon>=25&&s.lon<=45;}
 public static JourneyData.Route load(Context context,int city,CatalogStore.Item item,boolean force)throws IOException{
  return load(context,city,item,force,"");
 }
 public static JourneyData.Route load(Context context,int city,CatalogStore.Item item,boolean force,String variant)throws IOException{
  if(city==9)return BursaClient.route(item.code,item.name);if(city==3||city==8)return KentClient.route(city,item.code,item.name);if(CityRegistry.asis(city))return AsisCityClient.forCity(city).route(item.code,item.name);
  CatalogStore store=new CatalogStore(context);JourneyData.Route route=new JourneyData.Route();route.code=item.code;route.name=item.name;
  try{
   route.source=store.source(item.source);ArrayList<CatalogStore.Item> local=store.routeStops(item.id);
   if(city==0){
    JSONObject schedule=EgoSchedule.get(context,item.code,force);route.at=schedule.optLong("synced_at",0);route.stale=schedule.optBoolean("stale",false);route.source=schedule.optString("source")+"\nKonumlar: © OpenStreetMap contributors, ODbL.";
    JSONArray stops=schedule.getJSONArray("stops");HashSet<String> codes=new HashSet<String>();for(int i=0;i<stops.length();i++)codes.add(stops.getJSONObject(i).getString("code"));HashMap<String,CatalogStore.Item> locations=store.stops(city,codes);for(int i=0;i<stops.length();i++){JSONObject value=stops.getJSONObject(i);CatalogStore.Item known=locations.get(value.getString("code"));JourneyData.Station stop=known==null?new JourneyData.Station(value.getString("name"),Double.NaN,Double.NaN):station(known);stop.code=value.getString("code");stop.name=value.getString("name");route.stops.add(stop);}
   }else for(CatalogStore.Item stop:local)route.stops.add(station(stop));
   if(city==1&&item.type.toLowerCase(new Locale("tr","TR")).contains("otob")){
    String external=variant.length()>0?variant:store.external(item.id),direction=external.contains("_G_")?"G":external.contains("_D_")?"D":"G";
    try{ArrayList<JourneyData.Station> points=istanbul(context,item.code,external,item.name,force);if(points.size()>1){route.parts.add(points);route.schematic=false;route.at=System.currentTimeMillis();route.source="İETT güzergâhı / Otobüsüm Anlık API\nhttps://otobusum.metkm.win/";}route.stops.clear();route.stops.addAll(IettClient.routeStops(item.code,direction));}catch(IOException e){route.notice="Çevrimiçi yol çizgisi alınamadı. Yerel durak bağlantıları kullanılıyor.";}
   }
   if(route.parts.isEmpty()){ArrayList<JourneyData.Station> shape=store.shape(item.id);if(shape.size()>1){route.parts.add(shape);route.schematic=false;}}
   if(route.stops.isEmpty()){String code=item.code.split("_")[0];for(JourneyData.RailLine line:MetroData.lines(context,city))if(line.id.equals(code))route.stops.addAll(line.stations);}
   if(route.parts.isEmpty()){
    ArrayList<JourneyData.Station> part=new ArrayList<JourneyData.Station>();
    for(JourneyData.Station stop:route.stops)if(located(stop))part.add(stop);
    if(part.size()>1)route.parts.add(part);
   }
   for(JourneyData.Station stop:route.stops)if(located(stop))route.mappedStops++;
   if(route.mappedStops<route.stops.size())route.notice=(route.notice.length()>0?route.notice+"\n":"")+route.mappedStops+"/"+route.stops.size()+" durağın koordinatı var. "+(route.schematic?"Bilinen duraklar sırayla kesintisiz bağlanır; eksik durakların konumu ve gerçek yol güzergâhı gösterilmez.":"Koordinatı eksik duraklar işaretlenmez; yayımlanmış yol çizgisi korunur.");
   return route;
  }catch(JSONException e){throw new IOException("Hat verisi okunamadı");}finally{store.close();}
 }
 private static ArrayList<JourneyData.Station> istanbul(Context context,String line,String external,String name,boolean force)throws IOException{
  if(!line.matches("[A-Za-z0-9ÇĞİÖŞÜçğıöşü-]{1,16}"))throw new IOException("Geçerli İETT hat kodu gerekli");
  File directory=new File(context.getCacheDir(),"iett-route-json");directory.mkdirs();File file=new File(directory,URLEncoder.encode(line,"UTF-8")+".ijz");
  if(force||!file.exists()||System.currentTimeMillis()-file.lastModified()>86400000L){
   try{byte[] bytes=MapHttp.request("https://otobusum.metkm.win/v1/routes/"+URLEncoder.encode(line,"UTF-8")+"?city=istanbul",null,"",4194304);if(!StorageBudget.cache(context,file,bytes,true))return RouteJson.select(new ByteArrayInputStream(bytes),external,name);}catch(IOException e){if(!file.exists())throw e;}
  }
  InputStream in=new java.util.zip.GZIPInputStream(new FileInputStream(file),8192);try{return RouteJson.select(in,external,name);}finally{in.close();}
 }
}
