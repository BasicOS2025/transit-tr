package org.transittr;
import java.text.*;import java.util.*;import org.json.*;
final class SourceTools {
 static double number(JSONObject o,String name){try{return Double.parseDouble(o.optString(name,""));}catch(Exception e){return Double.NaN;}}
 static String text(JSONObject o,String name){return o.isNull(name)?"":o.optString(name,"").trim();}
 static boolean matches(String line,String filter){return filter.length()==0||line.equalsIgnoreCase(filter);}
 static long time(String s){for(String pattern:new String[]{"yyyy-MM-dd'T'HH:mm:ss","yyyy-MM-dd HH:mm:ss","yyyyMMddHHmmss"})try{SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.US);f.setTimeZone(TimeZone.getTimeZone("GMT+03:00"));return f.parse(s).getTime();}catch(Exception ignored){}return 0;}
 static String minutes(long seconds){return seconds>=0?((seconds+59)/60)+" dk":"Dakika yok";}
 static void sort(JourneyData.Snapshot s){Collections.sort(s.rows,new Comparator<JourneyData.Vehicle>(){public int compare(JourneyData.Vehicle a,JourneyData.Vehicle b){long x=a.etaSeconds>=0?a.etaSeconds:Long.MAX_VALUE,y=b.etaSeconds>=0?b.etaSeconds:Long.MAX_VALUE;return x<y?-1:x>y?1:0;}});}
}
