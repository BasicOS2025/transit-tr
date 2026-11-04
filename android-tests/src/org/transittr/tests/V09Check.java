package org.transittr.tests;
import android.app.*;import android.os.*;import android.content.*;import android.database.*;import android.database.sqlite.*;import android.view.*;import android.widget.*;import java.lang.reflect.*;
public class V09Check extends Instrumentation {
 private Activity a;private Spinner city;private EditText search;private ListView list;private SQLiteDatabase db;
 private Object field(String n)throws Exception{Field f=a.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(a);}
 private void mode(final int n)throws Exception{runOnMainSync(new Runnable(){public void run(){try{Field f=a.getClass().getDeclaredField("mode");f.setAccessible(true);f.setInt(a,n);search.setText("");Method m=a.getClass().getDeclaredMethod("show");m.setAccessible(true);m.invoke(a);}catch(Exception e){throw new RuntimeException(e);}}});waitForIdleSync();}
 private void report(String s){Bundle b=new Bundle();b.putString("stream",s+"\n");sendStatus(1,b);}
 private int count(String table,int i){Cursor c=db.rawQuery("select count(*) from "+table+" where city=?",new String[]{""+i});try{c.moveToFirst();return c.getInt(0);}finally{c.close();}}
 private View[] windows()throws Exception{Class c=Class.forName("android.view.WindowManagerImpl");Method m=c.getDeclaredMethod("getDefault");m.setAccessible(true);Object wm=m.invoke(null);Field f=c.getDeclaredField("mViews");f.setAccessible(true);return (View[])f.get(wm);}
 private View find(View v,String label,boolean edit){if(edit&&v instanceof EditText)return v;if(!edit&&v instanceof Button&&((Button)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View r=find(g.getChildAt(i),label,edit);if(r!=null)return r;}}return null;}
 private String dump(View v){String s=v instanceof TextView?((TextView)v).getText().toString():"";if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)s+="\n"+dump(g.getChildAt(i));}return s;}
 private String all()throws Exception{final String[] out={""};runOnMainSync(new Runnable(){public void run(){try{for(View root:windows())out[0]+=dump(root)+"\n";}catch(Exception e){throw new RuntimeException(e);}}});return out[0];}
 private void click(final String label,final String text){runOnMainSync(new Runnable(){public void run(){try{View[] roots=windows();for(int i=roots.length-1;i>=0;i--){View button=find(roots[i],label,false);if(button!=null){if(text!=null){EditText e=(EditText)find(roots[i],"",true);e.setText(text);}button.performClick();return;}}throw new RuntimeException("Button absent "+label);}catch(Exception e){throw new RuntimeException(e);}}});}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle result=new Bundle();int passed=0;try{
 a=startActivitySync(new Intent().setClassName("org.transittr","org.transittr.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();city=(Spinner)field("city");search=(EditText)field("search");list=(ListView)field("list");db=(SQLiteDatabase)field("db");
 report("ANKARA_CATALOG_ROUTES="+count("routes",0)+"; STOPS="+count("stops",0));if(count("routes",0)<600||count("stops",0)<2800)throw new AssertionError("Ankara catalog missing");
 Class<?> client=a.getClassLoader().loadClass("org.transittr.LiveTransit");Method ip=client.getMethod("istanbul",String.class,String.class),iz=client.getMethod("izmir",String.class,String.class),ko=client.getMethod("konya",String.class,String.class);
 String sample="<div class=\"line-list\"><div class=\"content\"><span>1</span><p>YÖN &amp; TEST <b>(21:00) 2 dk</b></p></div></div>";
 String value=(String)ip.invoke(null,sample,"123456");if(!value.contains("YÖN & TEST")||!value.contains("Tahmini varış:"))throw new AssertionError("IETT ETA parse");
 value=(String)ip.invoke(null,sample.replace("2 dk","2 dk B"),"123456");if(value.contains("Tahmini varış:"))throw new AssertionError("B must be uncertain");
 try{ip.invoke(null,"<html>Service error</html>","123456");throw new AssertionError("error HTML accepted");}catch(java.lang.reflect.InvocationTargetException expected){}
 value=(String)iz.invoke(null,"[{\"HatNumarasi\":446,\"HatAdi\":\"TEST\"}]","21050");if(value.contains("Kalan durak: 0")||!value.contains("Kalan durak bilgisi yok"))throw new AssertionError("unknown stop count fabricated");
 value=(String)iz.invoke(null,"[{\"HatNumarasi\":446,\"HatAdi\":\"TEST\",\"KalanDurakSayisi\":3}]","21050");if(!value.contains("Kalan durak: 3")||value.contains("Tahmini varış:"))throw new AssertionError("stops are not minutes");
 value=(String)ko.invoke(null,"<tbody id=\"searchBody\"><tr><td class=\"tdHatno\">84-B</td><td class=\"tdAdi\">TEST</td><td class=\"tdSure\">3 dk</td><td class=\"tdYon\">ALAADDİN</td></tr></tbody>","27");if(!value.contains("Tahmini varış: 3 dk"))throw new AssertionError("ATUS parse");
 report("PARSER_FIXTURES_PASS=6");runOnMainSync(new Runnable(){public void run(){city.setSelection(0);}});waitForIdleSync();mode(0);report("CAPTURE_CATALOG");Thread.sleep(6000);
 int[] ids={0,1,2,6};String[] codes={"10940","224672","21050","27"};
 for(int k=0;k<ids.length;k++){final int selected=ids[k];final String stop=codes[k];runOnMainSync(new Runnable(){public void run(){city.setSelection(selected);}});waitForIdleSync();mode(0);
 runOnMainSync(new Runnable(){public void run(){try{Method m=a.getClass().getDeclaredMethod("liveDialog",Integer.TYPE,String.class);m.setAccessible(true);m.invoke(a,selected,stop);}catch(Exception e){throw new RuntimeException(e);}}});waitForIdleSync();click("Kontrol et",null);
 String screen="";boolean ok=false;int peak=0;for(int n=0;n<180;n++){Thread.sleep(200);waitForIdleSync();android.os.Debug.MemoryInfo mem=new android.os.Debug.MemoryInfo();android.os.Debug.getMemoryInfo(mem);peak=Math.max(peak,mem.getTotalPss());screen=all();if(screen.contains("Alınma zamanı:")){ok=screen.contains(selected==2?"Kalan durak:":"Tahmini varış:")||selected==6&&screen.contains("Kaynak varış bilgisi:");break;}if(screen.contains("Canlı bilgi alınamadı:"))break;}
 if(ok)passed++;report("LIVE_CITY="+selected+"; STOP="+stop+"; SUCCESS="+ok+"; PEAK_PSS_KIB="+peak+"\n"+screen);report("CAPTURE_CITY_"+selected);Thread.sleep(6000);click("Kapat",null);waitForIdleSync();
 }
 result.putString("stream","DONE Android "+Build.VERSION.RELEASE+" API "+Build.VERSION.SDK_INT+"; LIVE_CITY_PASS="+passed+"/4\n");finish(passed==4?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
 }catch(Throwable t){java.io.StringWriter s=new java.io.StringWriter();t.printStackTrace(new java.io.PrintWriter(s));result.putString("stream","FAIL "+s.toString());finish(Activity.RESULT_CANCELED,result);}}
}
