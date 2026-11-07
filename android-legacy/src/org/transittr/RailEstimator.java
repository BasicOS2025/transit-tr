package org.transittr;

import android.content.Context;
import java.io.*;
import java.util.*;
import org.json.*;

/** Clock-based model only. It never represents an observed train or a live departure feed. */
public final class RailEstimator {
 public static final class Result {
  public final ArrayList<JourneyData.Vehicle> trains=new ArrayList<JourneyData.Vehicle>();
  public String explanation="",source="";
 }
 private static JSONObject data;
 private static synchronized JSONObject data(Context context)throws Exception{
  if(data==null){InputStream in=context.getAssets().open("rail-estimates.json");ByteArrayOutputStream out=new ByteArrayOutputStream();try{byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>262144)throw new IOException("Metro model sınırı");out.write(b,0,n);}}finally{in.close();}data=new JSONObject(new String(out.toByteArray(),"UTF-8"));EgoSchedule.applyRail(context,data);}return data;
 }
 public static long serviceStart(long now){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00"));c.setTimeInMillis(now);if(c.get(Calendar.HOUR_OF_DAY)<6)c.add(Calendar.DATE,-1);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
 public static synchronized void invalidate(){data=null;}
 private static int day(long start){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00"));c.setTimeInMillis(start);int d=c.get(Calendar.DAY_OF_WEEK);return d==Calendar.SATURDAY?1:d==Calendar.SUNDAY?2:0;}
 private static boolean night(long start){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00"));c.setTimeInMillis(start);int d=c.get(Calendar.DAY_OF_WEEK);return d==Calendar.FRIDAY||d==Calendar.SATURDAY;}
 private static JourneyData.RailLine find(ArrayList<JourneyData.RailLine> lines,String id){for(JourneyData.RailLine l:lines)if(id.equals(l.id))return l;return null;}
 private static JourneyData.RailLine corridor(ArrayList<JourneyData.RailLine> lines){JourneyData.RailLine r=new JourneyData.RailLine();r.id="M123";JourneyData.RailLine m2=find(lines,"M2"),m1=find(lines,"M1"),m3=find(lines,"M3");if(m2==null||m1==null||m3==null)return r;for(int i=m2.stations.size()-1;i>=0;i--)r.stations.add(m2.stations.get(i));for(int i=1;i<m1.stations.size();i++)r.stations.add(m1.stations.get(i));for(int i=1;i<m3.stations.size();i++)r.stations.add(m3.stations.get(i));return r;}
 private static double distance(JourneyData.Station a,JourneyData.Station b){double x=(a.lon-b.lon)*Math.cos((a.lat+b.lat)*Math.PI/360),y=a.lat-b.lat;return Math.max(.00001,Math.sqrt(x*x+y*y));}
 /** Interpolate travel with a fixed intermediate dwell, normalized to the published total trip time. */
 public static JourneyData.Vehicle position(JourneyData.RailLine route,int direction,double elapsed,int duration,int dwell){
  int count=route.stations.size();if(count<2||duration<=0||elapsed<0||elapsed>=duration)return null;
  double total=0;for(int i=0;i<count-1;i++)total+=distance(route.stations.get(i),route.stations.get(i+1));
  double travel=Math.max(duration*.5,duration-dwell*(count-2));double wait=(duration-travel)/Math.max(1,count-2),clock=0;
  for(int step=0;step<count-1;step++){int from=direction==0?step:count-1-step,to=direction==0?step+1:count-2-step;JourneyData.Station a=route.stations.get(from),b=route.stations.get(to);double leg=travel*distance(a,b)/total;
   if(elapsed<clock+leg||(step<count-2&&elapsed<clock+leg+wait)){
    double fraction=Math.min(1,Math.max(0,(elapsed-clock)/leg));JourneyData.Vehicle v=new JourneyData.Vehicle();v.fromStation=a.name;v.toStation=b.name;v.estimated=true;v.direction=direction==0?1:-1;v.line=route.id;v.lat=a.lat+(b.lat-a.lat)*fraction;v.lon=a.lon+(b.lon-a.lon)*fraction;v.name=route.stations.get(direction==0?count-1:0).name+" yönü";v.eta="≈ "+(int)Math.ceil(Math.max(0,clock+leg-elapsed)/60)+" dk";v.estimateDetail="Tahmini konum · GPS değil\n"+a.name+" → "+b.name+"\n"+(fraction==1?"Modelde istasyonda bekliyor":"Sonraki istasyona "+v.eta)+"\nSüreler mesafeye oranlanır; ara duruş varsayımı "+dwell+" sn.";return v;
   }clock+=leg+(step<count-2?wait:0);
  }return null;
 }
 public static Result estimate(Context context,int city,JourneyData.RailLine selected,ArrayList<JourneyData.RailLine> lines,long now){Result out=new Result();if(selected==null){out.explanation="Tahmini trenleri görmek için bir metro hattı seç.";return out;}try{
  JSONObject root=data(context);String id=city==0&&(selected.id.equals("M1")||selected.id.equals("M2")||selected.id.equals("M3"))?"M123":selected.id;JSONObject profile=null;JSONArray profiles=root.getJSONArray("profiles");for(int i=0;i<profiles.length();i++){JSONObject p=profiles.getJSONObject(i);if(p.getInt("city")==city&&p.getString("id").equals(id)){profile=p;break;}}
  if(profile==null){out.explanation="Bu hat için saat modeli yok; istasyon ağı gösteriliyor.";return out;}
  JourneyData.RailLine route=id.equals("M123")?corridor(lines):selected;if(route.stations.size()<2){out.explanation="Model için istasyon verisi eksik.";return out;}
  long start=serviceStart(now);double seconds=(now-start)/1000.0;int day=day(start),dwell=root.optInt("dwellSeconds",20);boolean scheduled="departures".equals(profile.getString("kind"));
  out.source=profile.getJSONArray("sources").getString(0);out.explanation=scheduled?"EGO kalkış çizelgesi · "+(day==0?"hafta içi":day==1?"cumartesi":"pazar")+" · "+(profile.optLong("synced_at",0)>0?new java.text.SimpleDateFormat("dd.MM.yyyy",Locale.US).format(new Date(profile.optLong("synced_at"))):"05.11.2026"):"Sıklık modeli · gerçek kalkış çizelgesi değil";
  out.explanation+="\n"+(id.equals("M123")?"M1–M2–M3 birleşik Koru–OSB hattı. ":"")+root.getString("assumptions");
  if(!scheduled)out.explanation+="\n"+profile.optString("note","");
  HashSet<String> allowed=new HashSet<String>();for(JourneyData.Station st:selected.stations)allowed.add(st.name);
  for(int direction=0;direction<2;direction++){int duration=profile.getJSONArray("tripSeconds").getInt(direction);ArrayList<Integer> departures=new ArrayList<Integer>();
   if(scheduled){JSONArray times=profile.getJSONArray("departures").getJSONArray(direction).getJSONArray(day);for(int i=0;i<times.length();i++)departures.add(Integer.valueOf(times.getInt(i)));}
   else{int first=profile.getInt("startSeconds"),last=profile.getInt("endSeconds"),headway=profile.getInt("headwaySeconds");for(int t=first;t<=last;t+=headway)departures.add(Integer.valueOf(t));if(profile.optBoolean("nightWeekend",false)&&night(start))for(int t=86400;t<108000;t+=1800)if(!departures.contains(Integer.valueOf(t)))departures.add(Integer.valueOf(t));}
   for(Integer time:departures){double elapsed=seconds-time.intValue();JourneyData.Vehicle v=position(route,direction,elapsed,duration,dwell);if(v==null)continue;
    if(id.equals("M123")&&!(allowed.contains(v.fromStation)&&allowed.contains(v.toStation)))continue;
    v.id="estimate:"+city+":"+id+":"+start+":"+direction+":"+time;v.line=selected.id;v.estimateDetail+="\n\n"+out.explanation;out.trains.add(v);if(out.trains.size()>=128)return out;
   }
  }
 }catch(Exception e){out.explanation="Saat modeli okunamadı; yalnızca istasyon ağı gösteriliyor.";}return out;}
}
