package org.transittr;
import java.io.IOException;import java.util.*;import org.json.*;
/** EGO Cepte live stop prediction endpoint. GPS comes from the separate mobile bus feed. */
public final class EgoRealtime {
 public static JourneyData.Snapshot parse(String json,String filter,JourneyData.Snapshot gps)throws IOException{try{
  JSONObject root=new JSONObject(json);if(!root.optBoolean("success",false))throw new IOException("EGO varış servisi başarısız");JSONArray rows=root.optJSONArray("arrivals");if(rows==null||rows.length()>200)throw new IOException("EGO varış biçimi değişmiş");
  JourneyData.Snapshot out=new JourneyData.Snapshot();out.source=root.optString("source","").toLowerCase(Locale.US).contains("live")?"EGO Cepte · canlı durak tahmini":"EGO Cepte · durak tahmini";out.separateMapRows=true;if(gps!=null)out.mapRows.addAll(gps.mapVehicles());
  for(int i=0;i<rows.length();i++){JSONObject a=rows.getJSONObject(i);String line=a.optString("routeId","").trim(),bus=a.optString("busId","").trim();if(line.length()==0||line.length()>32||bus.length()>80||filter.length()>0&&!line.equalsIgnoreCase(filter))continue;
   if(a.isNull("remainingMinutes"))continue;int minutes=a.optInt("remainingMinutes",-1);if(minutes<0||minutes>1440)continue;JourneyData.Vehicle v=null;if(gps!=null)for(JourneyData.Vehicle old:gps.rows)if(old.id.equals("ego:"+bus)&&old.line.equalsIgnoreCase(line)){v=old;break;}
   if(v==null){v=new JourneyData.Vehicle();v.id="ego:"+bus;v.line=line;if(gps!=null)for(JourneyData.Vehicle old:gps.rows)if(old.line.equalsIgnoreCase(line)&&old.name.length()>0){v.name=old.name;break;}}
   v.signalAgeSeconds=a.isNull("signalAgeSeconds")?-1:a.optInt("signalAgeSeconds",-1);v.etaSeconds=minutes*60L;v.eta=minutes+" dk";v.etaIsStopArrival=true;out.rows.add(v);
  }
  Collections.sort(out.rows,new Comparator<JourneyData.Vehicle>(){public int compare(JourneyData.Vehicle a,JourneyData.Vehicle b){return a.etaSeconds<b.etaSeconds?-1:a.etaSeconds>b.etaSeconds?1:0;}});
  int age=root.optInt("snapshotAgeSeconds",-1);out.sourceAgeSeconds=age;out.message="Dakikalar EGO Cepte'nin durak için hesapladığı tahminlerdir."+(age>=0?" Kaynak görüntüsünün yaşı: "+age+" sn.":"");if(out.rows.isEmpty())out.emptyMessage="EGO bu durak için yaklaşan araç tahmini döndürmedi.";return out;
 }catch(JSONException e){throw new IOException("EGO varış yanıtı okunamadı");}}
 public static JourneyData.Snapshot query(String stop,String filter,JourneyData.Snapshot gps)throws IOException{if(!stop.matches("[0-9]{5}"))throw new IOException("5 haneli durak gerekli");return parse(ApiHttp.get("https://egocptngapi.ego.gov.tr/api/arrivals?stopId="+stop),filter,gps);}
}
