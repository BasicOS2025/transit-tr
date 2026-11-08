package org.transittr;

import android.content.Context;
import android.text.Html;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import org.json.*;

/** Published EGO timetables, cached per line; schedule facts are never live bus ETAs. */
public final class EgoSchedule {
 private static String clean(String value){return Html.fromHtml(value.replaceAll("<[^>]*>"," ")).toString().replace('\u00a0',' ').replaceAll("\\s+"," ").trim();}
 private static String field(String header,String label){Pattern pattern=Pattern.compile("<td[^>]*>\\s*"+Pattern.quote(label)+"\\s*</td>\\s*<td[^>]*>\\s*:\\s*</td>\\s*<td[^>]*>(.*?)</td>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL);Matcher m=pattern.matcher(header);return m.find()?clean(m.group(1)):"";}
 public static JSONObject parse(String page,String code,long at)throws IOException{
  try{
   int marker=page.indexOf("<!-- Saat Tablosu -->");if(marker<0)throw new IOException("EGO saat tablosu bulunamadı");
   String header=page.substring(0,marker),body=page.substring(marker);if(!field(header,"Hat No").equals(code))throw new IOException("EGO hat kimliği eşleşmiyor");
   JSONArray days=new JSONArray();Matcher cells=Pattern.compile("<td[^>]*class=[\"'][^\"']*hs-times[^\"']*[\"'][^>]*>(.*?)</td>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(body);
   while(cells.find()){JSONArray entries=new JSONArray();for(String part:cells.group(1).split("(?i)<br\\s*/?>")){String value=clean(part);if(value.matches("[0-9]{2}:[0-9]{2}(?:\\s.*)?"))entries.put(value.replaceFirst("[ -]+$",""));}days.put(entries);}
   if(days.length()!=3)throw new IOException("EGO üç günlük saat sütunu okunamadı");
   JSONArray stops=new JSONArray();int start=body.indexOf("<!-- Geçtiği Güzergahlar -->");if(start>=0){Matcher rows=Pattern.compile("<tr[^>]*>(.*?)</tr>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(body.substring(start));while(rows.find()){Matcher values=Pattern.compile("<td[^>]*>(.*?)</td>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(rows.group(1));ArrayList<String> fields=new ArrayList<String>();while(values.find())fields.add(clean(values.group(1)));if(fields.size()>=3&&fields.get(0).matches("[0-9]+")&&fields.get(1).matches("[A-Za-z0-9_-]{1,20}")){JSONObject stop=new JSONObject();stop.put("seq",Integer.parseInt(fields.get(0)));stop.put("code",fields.get(1));stop.put("name",fields.get(2));stops.put(stop);}}}
   JSONObject result=new JSONObject();result.put("code",code);result.put("name",field(header,"Hat Adı"));result.put("origin",field(header,"Kalkış Yeri"));result.put("destination",field(header,"Varış Yeri"));String duration=field(header,"Süresi");Matcher minutes=Pattern.compile("^([0-9]+)").matcher(duration);result.put("duration_minutes",minutes.find()?Integer.parseInt(minutes.group(1)):JSONObject.NULL);result.put("days",days);result.put("stops",stops);result.put("synced_at",at);result.put("source","https://www.ego.gov.tr/hareketsaatleri?hat_no="+code);return result;
  }catch(JSONException e){throw new IOException("EGO saat verisi okunamadı");}
 }
 private static void validate(String code)throws IOException{if(!code.matches("[A-Za-z0-9-]{1,16}"))throw new IOException("Geçerli EGO hat kodu gerekli");}
 private static JSONObject read(InputStream in)throws IOException{try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>262144)throw new IOException("Tarife önbellek sınırı");out.write(b,0,n);}return new JSONObject(new String(out.toByteArray(),"UTF-8"));}catch(JSONException e){throw new IOException("Tarife önbelleği okunamadı");}finally{in.close();}}
 public static JSONObject cached(Context context,String code)throws IOException{
  validate(code);try{InputStream in=ScheduleCache.open(context,code);if(in!=null)return read(in);}catch(IOException ignored){}
  try{return read(context.getAssets().open("ego-times/"+code+".json"));}catch(IOException e){return null;}
 }
 public static JSONObject get(Context context,String code,boolean force)throws IOException{
  validate(code);JSONObject old=cached(context,code);long now=System.currentTimeMillis();if(!force&&old!=null&&now-old.optLong("synced_at",0)<86400000L)return old;
  try{JSONObject fresh=parse(MapHttp.text("https://www.ego.gov.tr/hareketsaatleri?hat_no="+URLEncoder.encode(code,"UTF-8"),null,""),code,now);ScheduleCache.write(context,code,fresh.toString().getBytes("UTF-8"));RailEstimator.invalidate();return fresh;
  }catch(IOException e){if(old==null)throw e;try{JSONObject fallback=new JSONObject(old.toString());fallback.put("stale",true);fallback.put("sync_error",e.getMessage());return fallback;}catch(JSONException impossible){throw e;}}
 }
 public static void applyRail(Context context,JSONObject root){
  try{
   JSONArray profiles=root.getJSONArray("profiles");String[][] codes={{"M1-D","M1-G"},{"M4-G","M4-D"},{"A1-D","A1-G"}};String[] ids={"M123","M4","A1"};
   for(int p=0;p<ids.length;p++){JSONObject profile=null;for(int i=0;i<profiles.length();i++){JSONObject candidate=profiles.getJSONObject(i);if(candidate.getInt("city")==0&&candidate.getString("id").equals(ids[p]))profile=candidate;}if(profile==null)continue;JSONArray directions=new JSONArray(),durations=new JSONArray();long oldest=Long.MAX_VALUE;boolean complete=true;
    for(String code:codes[p]){JSONObject value=cached(context,code);if(value==null){complete=false;break;}oldest=Math.min(oldest,value.optLong("synced_at",0));JSONArray dayTypes=new JSONArray(),days=value.getJSONArray("days");for(int d=0;d<3;d++){TreeSet<Integer> times=new TreeSet<Integer>();JSONArray entries=days.getJSONArray(d);for(int i=0;i<entries.length();i++){String time=entries.getString(i);if(!time.matches("[0-9]{2}:[0-9]{2}"))continue;int seconds=Integer.parseInt(time.substring(0,2))*3600+Integer.parseInt(time.substring(3,5))*60;if(seconds<21600)seconds+=86400;times.add(Integer.valueOf(seconds));}JSONArray sorted=new JSONArray();for(Integer time:times)sorted.put(time.intValue());dayTypes.put(sorted);}directions.put(dayTypes);durations.put(value.optInt("duration_minutes",0)*60);}
    if(complete&&durations.length()==2&&durations.getInt(0)>0&&durations.getInt(1)>0){profile.put("departures",directions);profile.put("tripSeconds",durations);profile.put("synced_at",oldest);}
   }
  }catch(Exception ignored){}
 }
 public static String description(JSONObject value,int day)throws IOException{
  try{JSONArray entries=value.getJSONArray("days").getJSONArray(day);StringBuilder text=new StringBuilder(new String[]{"Hafta içi","Cumartesi","Pazar"}[day]+"\n"+value.optString("name")).append("\n\n");for(int i=0;i<entries.length();i++)text.append(entries.getString(i)).append('\n');if(entries.length()==0)text.append("Kaynak bu gün için saat yayımlamıyor.");return text.toString();}catch(JSONException e){throw new IOException("Saat sütunu okunamadı");}
 }
}
