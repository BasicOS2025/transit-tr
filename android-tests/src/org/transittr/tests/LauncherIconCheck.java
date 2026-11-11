package org.transittr.tests;
import android.app.Instrumentation;import android.os.Bundle;import android.content.Context;import android.content.pm.ApplicationInfo;import android.graphics.drawable.Drawable;import java.lang.reflect.Method;
public final class LauncherIconCheck extends Instrumentation {
 public void onCreate(Bundle args){super.onCreate(args);start();}
 public void onStart(){Bundle result=new Bundle();try{Context c=getTargetContext();ApplicationInfo info=c.getApplicationInfo();Drawable icon=info.loadIcon(c.getPackageManager());Class<?> adaptive=Class.forName("android.graphics.drawable.AdaptiveIconDrawable");if(!adaptive.isInstance(icon))throw new AssertionError("Launcher icon is not adaptive: "+icon.getClass());Method mono=adaptive.getMethod("getMonochrome");Object layer=mono.invoke(icon);if(layer==null)throw new AssertionError("Android did not load the monochrome layer");result.putString("stream","PASS Android adaptive icon and monochrome layer\n");finish(-1,result);}catch(Throwable e){java.io.StringWriter s=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(s));result.putString("stream","FAIL "+s);finish(0,result);}}
}
