package org.transittr;
import android.content.Context;import java.io.*;import java.net.URLEncoder;import org.json.*;
/** A stop pin always comes from stop coordinates, never a vehicle position. */
public final class StopResolver {
 public static JourneyData.Station resolve(Context context,int city,String code)throws IOException {
  CatalogStore store=new CatalogStore(context);
  try{CatalogStore.Item item=store.stop(city,code);if(item==null&&city==6)item=store.uniqueKonyaStop(code);if(item!=null&&item.hasPosition()){JourneyData.Station s=new JourneyData.Station(item.name,item.lat,item.lon);s.code=code;return s;}}finally{store.close();}
  if(city==9)return BursaClient.stop(code);if(city==3||city==8)return KentClient.stop(city,code);if(CityRegistry.asis(city))return AsisCityClient.forCity(city).stop(code);if(city!=1&&city!=2)return null;
  try{JSONObject root=new JSONObject(MapHttp.text("https://otobusum.metkm.win/v1/stop/"+URLEncoder.encode(code,"UTF-8")+"?city="+(city==1?"istanbul":"izmir"),null,""));JSONObject value=root.getJSONObject("stop");double lat=value.getDouble("lat"),lon=value.getDouble("lng");if(!(lat>=35&&lat<=43&&lon>=25&&lon<=45))return null;JourneyData.Station s=new JourneyData.Station(value.optString("name",code),lat,lon);s.code=code;return s;}catch(JSONException e){throw new IOException("Durak konumu okunamadı");}
 }
}
