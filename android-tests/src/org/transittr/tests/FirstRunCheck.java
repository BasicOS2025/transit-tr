package org.transittr.tests;
import android.app.*;import android.content.*;import android.os.*;import java.lang.reflect.*;
/** Exercise actual one-time notice buttons; preserve the user's favourites and prior acknowledgement. */
public final class FirstRunCheck extends Instrumentation {
 private Activity activity;private SharedPreferences prefs;private boolean had,original;
 public void onCreate(Bundle b){super.onCreate(b);start();}
 private void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 private Object field(String name)throws Exception{Field f=activity.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
 private void status(String text){Bundle b=new Bundle();b.putString("stream",text+"\n");sendStatus(1,b);}
 private void closeActivity(){if(activity!=null){runOnMainSync(new Runnable(){public void run(){activity.finish();}});waitForIdleSync();activity=null;}}
 private void launch(String name){activity=startActivitySync(new Intent().setClassName("org.transittr","org.transittr."+name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();}
 private void click(final int which)throws Exception{final AlertDialog dialog=(AlertDialog)field("firstRunDialog");check(dialog!=null&&dialog.isShowing(),"notice visible");runOnMainSync(new Runnable(){public void run(){dialog.getButton(which).performClick();}});waitForIdleSync();}
 public void onStart(){Bundle result=new Bundle();boolean pass=false;try{
  prefs=getTargetContext().getSharedPreferences("legal-first-run-v1",0);had=prefs.contains("acknowledged");original=prefs.getBoolean("acknowledged",false);String favorites=getTargetContext().getSharedPreferences("widget-favorites-v1",0).getString("entries","[]");
  prefs.edit().remove("acknowledged").commit();launch("TransitActivity");check(field("map")==null,"no map before acknowledgement");check(field("firstRunDialog")!=null,"first launch notice shown");status("CAPTURE=FIRST_RUN_NOTICE");Thread.sleep(6500);
  click(DialogInterface.BUTTON_NEGATIVE);check(activity.isFinishing(),"exit finishes activity");check(!prefs.getBoolean("acknowledged",false),"exit does not acknowledge");closeActivity();
  launch("TransitActivity");check(field("firstRunDialog")!=null,"notice returns after exit");click(DialogInterface.BUTTON_POSITIVE);check(prefs.getBoolean("acknowledged",false),"continue persists acknowledgement");check(field("map")!=null,"normal screen opens after acknowledgement");status("CAPTURE=AFTER_NOTICE");Thread.sleep(6500);closeActivity();
  launch("TransitActivity");check(field("firstRunDialog")==null,"second launch skips notice");check(field("map")!=null,"second launch opens normally");closeActivity();
  prefs.edit().remove("acknowledged").commit();launch("WidgetSetupActivity");check(field("city")==null,"widget setup waits for first notice");click(DialogInterface.BUTTON_POSITIVE);check(field("city")!=null,"widget setup opens after notice");closeActivity();
  check(favorites.equals(getTargetContext().getSharedPreferences("widget-favorites-v1",0).getString("entries","[]")),"user favourites preserved");pass=true;result.putString("stream","PASS Android "+Build.VERSION.RELEASE+" API "+Build.VERSION.SDK_INT+"; FIRST_LAUNCH=true; EXIT_NOT_ACKNOWLEDGED=true; CONTINUE_PERSISTED=true; REOPEN_SKIPS=true; WIDGET_SETUP=true; FAVORITES_PRESERVED=true\n");
 }catch(Throwable e){java.io.StringWriter s=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(s));result.putString("stream","FAIL "+s);}finally{closeActivity();if(prefs!=null){SharedPreferences.Editor e=prefs.edit();if(had)e.putBoolean("acknowledged",original);else e.remove("acknowledged");e.commit();}}finish(pass?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
