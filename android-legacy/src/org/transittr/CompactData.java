package org.transittr;
import android.content.Context;import java.io.*;import java.util.*;import java.util.zip.*;import org.json.*;
/** Small compressed assets are opened one route/stop at a time, never expanded to disk. */
public final class CompactData {
 public static JSONArray read(Context c,String folder,int id)throws IOException{
  InputStream raw;try{raw=c.getAssets().open(folder+"/"+id+".dbz");}catch(FileNotFoundException missing){return new JSONArray();}catch(IOException missing){return new JSONArray();}
  InputStream in=new GZIPInputStream(raw,8192);try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>524288)throw new IOException("Veri boyutu sınırı");out.write(buffer,0,n);}return new JSONArray(new String(out.toByteArray(),"UTF-8"));}catch(JSONException e){throw new IOException("Sıkıştırılmış veri okunamadı");}finally{in.close();}
 }
 public static ArrayList<JourneyData.Station> shape(Context c,int id)throws IOException{JSONArray a=read(c,"route-shapes",id);if(a.length()>20000)throw new IOException("Rota nokta sınırı");ArrayList<JourneyData.Station> points=new ArrayList<JourneyData.Station>();try{for(int i=0;i<a.length();i++){JSONArray p=a.getJSONArray(i);double lat=p.getDouble(0),lon=p.getDouble(1);if(!(lat>=35&&lat<=43&&lon>=25&&lon<=45))throw new IOException("Rota koordinatı geçersiz");points.add(new JourneyData.Station("",lat,lon));}}catch(JSONException e){throw new IOException("Rota okunamadı");}return points;}
}
