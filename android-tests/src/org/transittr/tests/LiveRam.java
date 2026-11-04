package org.transittr.tests;
import android.app.*;import android.os.*;import android.content.*;
public class LiveRam extends Instrumentation {
 private volatile boolean measuring;private int peak;private int samples;
 private synchronized void sample(){Debug.MemoryInfo m=new Debug.MemoryInfo();Debug.getMemoryInfo(m);peak=Math.max(peak,m.getTotalPss());samples++;}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle out=new Bundle();boolean success=false;Thread sampler=null;long began=System.currentTimeMillis();String detail="";try{
 sample();measuring=true;sampler=new Thread(new Runnable(){public void run(){while(measuring){sample();try{Thread.sleep(100);}catch(InterruptedException e){break;}}}});sampler.start();
 Activity a=startActivitySync(new Intent().setClassName("org.transittr","org.transittr.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();sample();
 String live=(String)a.getClassLoader().loadClass("org.transittr.EgoMobile").getMethod("query",String.class).invoke(null,"10001");sample();success=live.indexOf("Alınma zamanı:")>=0&&live.indexOf("Tahmini varış:")>=0;detail=live;
 }catch(Throwable e){Throwable cause=e.getCause()==null?e:e.getCause();detail=cause.toString();}finally{sample();measuring=false;if(sampler!=null){sampler.interrupt();try{sampler.join(2000);}catch(Exception ignored){}}}
 out.putString("stream","ANDROID_RELEASE="+Build.VERSION.RELEASE+"; API="+Build.VERSION.SDK_INT+"; LIVE_SUCCESS="+success+"; PEAK_PSS_KIB="+peak+"; SAMPLES="+samples+"; ELAPSED_MS="+(System.currentTimeMillis()-began)+"; UNDER_200MB="+(peak*1024L<200000000L)+"\n"+detail+"\n"+(success?"PASS successful live-query RAM measurement":"FAIL successful live-query RAM measurement NOT obtained")+"\n");finish(success?Activity.RESULT_OK:Activity.RESULT_CANCELED,out);
 }
}
