package org.transittr;
import android.content.Context;import java.io.*;
/** One compressed APK asset avoids hundreds of repeated timetable strings and ZIP headers. */
public final class EgoScheduleAsset {
 public static InputStream open(Context context,String code)throws IOException{
  DataInputStream in=new DataInputStream(new BufferedInputStream(new java.util.zip.GZIPInputStream(context.getAssets().open("ego-schedules.dbz"),8192),8192));
  try{int count=in.readInt();if(count<0||count>2000)throw new IOException("Saat dizini sınırı");for(int i=0;i<count;i++){String name=in.readUTF();int size=in.readInt();if(size<0||size>262144)throw new IOException("Saat kaydı sınırı");if(name.equals(code)){byte[] raw=new byte[size];in.readFully(raw);return new ByteArrayInputStream(raw);}int left=size;while(left>0){int skipped=in.skipBytes(left);if(skipped==0){if(in.read()==-1)throw new EOFException();skipped=1;}left-=skipped;}}throw new FileNotFoundException(code);
  }finally{in.close();}
 }
}
