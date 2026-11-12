package org.transittr.tests;
import android.app.Instrumentation;import android.os.Bundle;import java.lang.reflect.*;import java.util.*;
public final class EgoRealtimeCheck extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle out=new Bundle();boolean pass=false;try{
  ClassLoader l=getTargetContext().getClassLoader();Class<?> snapshot=l.loadClass("org.transittr.JourneyData$Snapshot"),vehicle=l.loadClass("org.transittr.JourneyData$Vehicle"),client=l.loadClass("org.transittr.EgoRealtime");Object gps=snapshot.newInstance(),old=vehicle.newInstance();vehicle.getField("line").set(old,"481");vehicle.getField("id").set(old,"ego:07-451");vehicle.getField("name").set(old,"Kızılay");vehicle.getField("lat").setDouble(old,39.90);vehicle.getField("lon").setDouble(old,32.85);((ArrayList)snapshot.getField("rows").get(gps)).add(old);
  String json="{\"success\":true,\"snapshotAgeSeconds\":1,\"arrivals\":[{\"routeId\":\"481\",\"busId\":\"07-451\",\"remainingMinutes\":4},{\"routeId\":\"263-7\",\"busId\":\"b\",\"remainingMinutes\":9},{\"routeId\":\"481\",\"busId\":\"missing\",\"remainingMinutes\":null}]}";
  Method parse=client.getMethod("parse",String.class,String.class,snapshot);Object result=parse.invoke(null,json,"481",gps);ArrayList rows=(ArrayList)snapshot.getField("rows").get(result),map=(ArrayList)snapshot.getField("mapRows").get(result);if(rows.size()!=1||map.size()!=1)throw new AssertionError("filter or map mismatch");if(!"4 dk".equals(vehicle.getField("eta").get(rows.get(0))))throw new AssertionError("source minutes not preserved");if(Double.isNaN(vehicle.getField("lat").getDouble(rows.get(0))))throw new AssertionError("matching GPS lost");
  Object noGps=parse.invoke(null,json,"263-7",null);ArrayList noGpsRows=(ArrayList)snapshot.getField("rows").get(noGps);if(noGpsRows.size()!=1||!Double.isNaN(vehicle.getField("lat").getDouble(noGpsRows.get(0))))throw new AssertionError("GPS must not be invented");
  pass=true;out.putString("stream","PASS EGO response parsing; filtering, source minutes, GPS matching and missing GPS\n");
 }catch(Throwable e){java.io.StringWriter w=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(w));out.putString("stream","FAIL "+w);}finish(pass?-1:0,out);}
}
