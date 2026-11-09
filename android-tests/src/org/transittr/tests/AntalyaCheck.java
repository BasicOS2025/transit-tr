package org.transittr.tests;
import android.app.*;import android.os.*;import android.content.*;import android.widget.*;import java.lang.reflect.*;import java.util.*;
/** Real public transport requests on each supported Android runtime. */
public final class AntalyaCheck extends Instrumentation {
 private Activity activity;
 private Object field(String n)throws Exception{Field f=activity.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(activity);}
 private void set(String n,Object v)throws Exception{Field f=activity.getClass().getDeclaredField(n);f.setAccessible(true);f.set(activity,v);}
 private void call(String n)throws Exception{Method m=activity.getClass().getDeclaredMethod(n);m.setAccessible(true);m.invoke(activity);}
 private void status(String s){Bundle b=new Bundle();b.putString("stream",s+"\n");sendStatus(1,b);}
 private void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle result=new Bundle();boolean passed=false;try{
  activity=startActivitySync(new Intent().setClassName("org.transittr","org.transittr.TransitActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
  Class<?> kent=activity.getClassLoader().loadClass("org.transittr.KentClient");Object route=kent.getMethod("route",Integer.TYPE,String.class,String.class).invoke(null,8,"VL13","VL13");ArrayList parts=(ArrayList)route.getClass().getField("parts").get(route),stops=(ArrayList)route.getClass().getField("stops").get(route);check(parts.size()>0&&stops.size()>5,"signed route geometry and stops");status("ANTALYA_ROUTE_STOPS="+stops.size());
  runOnMainSync(new Runnable(){public void run(){try{call("stopTracking");set("city",Integer.valueOf(8));set("screen",Integer.valueOf(0));set("snapshot",null);call("build");((EditText)field("stopInput")).setText("12406");((EditText)field("lineInput")).setText("");((Button)field("start")).performClick();}catch(Exception e){throw new RuntimeException(e);}}});
  Object live=null;for(int i=0;i<250;i++){Thread.sleep(200);live=field("snapshot");if(live!=null)break;}check(live!=null,"live UI request completed");ArrayList rows=(ArrayList)live.getClass().getField("rows").get(live);int positions=((Integer)live.getClass().getMethod("positionCount").invoke(live)).intValue(),etas=0;for(Object row:rows)if(((Long)row.getClass().getField("etaSeconds").get(row)).longValue()>=0)etas++;check(positions>0&&etas>0,"actual GPS and stop minutes");status("ANTALYA_LIVE_GPS="+positions+"; ETA_ROWS="+etas);status("CAPTURE=ANTALYA_LIVE");Thread.sleep(5000);passed=true;result.putString("stream","PASS Android "+Build.VERSION.RELEASE+" API "+Build.VERSION.SDK_INT+"; Antalya live GPS, minutes, route and UI\n");
 }catch(Throwable e){java.io.StringWriter w=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(w));result.putString("stream","FAIL Android "+Build.VERSION.SDK_INT+"\n"+w);}finish(passed?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
