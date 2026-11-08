package org.transittr.tests;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import java.lang.reflect.*;import java.util.*;
/** Real map rendering at mismatched bitmap/canvas densities, including tile boundaries. */
public final class MapSeamCheck extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 private Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 public void onStart(){final Bundle result=new Bundle();final Throwable[] failure=new Throwable[1];
  final Activity a=startActivitySync(new Intent().setClassName("org.transittr","org.transittr.TransitActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  runOnMainSync(new Runnable(){public void run(){try{
   Class<?> type=a.getClassLoader().loadClass("org.transittr.OsmMapView");
   for(int density:new int[]{120,160,240,320}){
    View map=(View)type.getConstructor(Context.class).newInstance(a);type.getMethod("pause",Boolean.TYPE).invoke(map,true);type.getMethod("center",Double.TYPE,Double.TYPE,Integer.TYPE).invoke(map,0.0,0.0,4);map.layout(0,0,512,512);
    Bitmap tile=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);tile.setDensity(160);
    for(int y=0;y<256;y++)for(int x=0;x<256;x++)tile.setPixel(x,y,Color.rgb(x,y,(x+y)/2));
    Field loaded=type.getDeclaredField("tilesLoaded");loaded.setAccessible(true);loaded.setInt(map,4);Map memory=(Map)field(map,"memory");for(int x=7;x<=9;x++)for(int y=7;y<=9;y++)memory.put("4/"+x+"/"+y,tile);
    Bitmap actual=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888),expected=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);actual.setDensity(density);expected.setDensity(density);
    Canvas c=new Canvas(actual);map.draw(c);Canvas ref=new Canvas(expected);Paint paint=(Paint)field(map,"tilePaint");for(int x=0;x<2;x++)for(int y=0;y<2;y++)ref.drawBitmap(tile,null,new RectF(x*256,y*256,x*256+256,y*256+256),paint);
    for(int y:new int[]{20,100,200,254,255,256,257,300,400})for(int x:new int[]{20,100,200,254,255,256,257,300,400})if(actual.getPixel(x,y)!=expected.getPixel(x,y))throw new AssertionError("Tile scale/seam at density "+density+" pixel "+x+","+y);
    type.getMethod("close").invoke(map);tile.recycle();actual.recycle();expected.recycle();
   }
  }catch(Throwable e){failure[0]=e;}}});
  if(failure[0]==null)result.putString("stream","PASS Android "+Build.VERSION.RELEASE+"; TILE_SEAMS=false; BITMAP_DENSITY=160; CANVAS_DENSITIES=120,160,240,320; SHARED_BUS_CATALOG_MAP=true\n");else{java.io.StringWriter s=new java.io.StringWriter();failure[0].printStackTrace(new java.io.PrintWriter(s));result.putString("stream","FAIL "+s);}
  finish(failure[0]==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
 }
}
