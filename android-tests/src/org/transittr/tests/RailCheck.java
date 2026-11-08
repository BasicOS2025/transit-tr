package org.transittr.tests;
import android.app.*;import android.content.*;import android.os.*;import java.lang.reflect.*;import java.util.*;
public final class RailCheck extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 private void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 private void status(String s){Bundle b=new Bundle();b.putString("stream",s+"\n");sendStatus(1,b);}
 public void onStart(){Bundle out=new Bundle();boolean pass=false;try{
  final Activity a=startActivitySync(new Intent().setClassName("org.transittr","org.transittr.TransitActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));ClassLoader loader=a.getClassLoader();Class<?> data=loader.loadClass("org.transittr.MetroData"),line=loader.loadClass("org.transittr.JourneyData$RailLine"),engine=loader.loadClass("org.transittr.RailEstimator");ArrayList all=(ArrayList)data.getMethod("lines",Context.class,Integer.TYPE).invoke(null,a,1);check(all.size()==26,"26 Istanbul entries including branch");HashMap<String,Object> byId=new HashMap<String,Object>();for(Object l:all)byId.put((String)line.getField("id").get(l),l);
  for(String code:new String[]{"M2-S","B1","B2","F2","F3","T2","T6"})check(byId.containsKey(code),"missing "+code);
  check(((ArrayList)line.getField("stations").get(byId.get("M5"))).size()==24,"M5 extension 24 stations");check(((ArrayList)line.getField("stations").get(byId.get("M2"))).size()==15,"M2 main excludes Seyrantepe detour");check(((ArrayList)line.getField("stations").get(byId.get("B1"))).size()==43,"Marmaray 43 stations");
  Method estimate=engine.getMethod("estimate",Context.class,Integer.TYPE,line,ArrayList.class,Long.TYPE);for(String id:new String[]{"B1","B2","T6","F2","F3","T2","M2-S"}){Object r=estimate.invoke(null,a,1,byId.get(id),all,System.currentTimeMillis());check(((ArrayList)r.getClass().getField("trains").get(r)).isEmpty(),"no invented positions "+id);}
  runOnMainSync(new Runnable(){public void run(){try{Field city=a.getClass().getDeclaredField("city"),screen=a.getClass().getDeclaredField("screen");city.setAccessible(true);screen.setAccessible(true);city.setInt(a,1);screen.setInt(a,1);Method build=a.getClass().getDeclaredMethod("build");build.setAccessible(true);build.invoke(a);}catch(Exception e){throw new RuntimeException(e);}}});waitForIdleSync();status("CAPTURE=ISTANBUL_RAIL_UPDATED");Thread.sleep(6500);
  pass=true;out.putString("stream","PASS Android "+Build.VERSION.RELEASE+"; ISTANBUL_RAIL_ENTRIES=26; M5_STATIONS=24; M2_MAIN=15; MARMARAY=43; NO_INVENTED_POSITIONS=true\n");
 }catch(Throwable e){java.io.StringWriter s=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(s));out.putString("stream","FAIL "+s);}finish(pass?Activity.RESULT_OK:Activity.RESULT_CANCELED,out);}
}
