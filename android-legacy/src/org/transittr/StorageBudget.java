package org.transittr;
import android.content.Context;import java.io.*;import java.util.*;import java.util.zip.*;
/** One shared disk budget for every map/route cache, including allocated block rounding. */
public final class StorageBudget {
 public static final long CACHE_LIMIT=131072;
 private static boolean prepared;
 private static long allocated(File f){return f.isDirectory()?4096:((f.length()+4095)/4096)*4096;}
 private static void files(File root,File dir,ArrayList<File> output)throws IOException{File[] list=dir.listFiles();if(list==null)return;String prefix=root.getCanonicalPath()+File.separator;for(File f:list){if(!f.getCanonicalPath().startsWith(prefix))continue;if(f.isDirectory())files(root,f,output);else output.add(f);}}
 private static long size(File root)throws IOException{if(!root.exists())return 0;long total=allocated(root);File[] list=root.listFiles();if(list!=null)for(File f:list){if(!f.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))continue;total+=f.isDirectory()?size(f):allocated(f);}return total;}
 private static void trim(File directory,long limit,long reserve)throws IOException{directory.mkdirs();ArrayList<File> files=new ArrayList<File>();files(directory,directory,files);Collections.sort(files,new Comparator<File>(){public int compare(File a,File b){return a.lastModified()<b.lastModified()?-1:a.lastModified()>b.lastModified()?1:0;}});long bytes=size(directory);int count=files.size();for(File file:files){if(bytes+reserve<=limit&&count<=96)break;long amount=allocated(file);if(file.delete()){bytes-=amount;count--;}}}
 private static byte[] gzip(byte[] bytes)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();GZIPOutputStream zip=new GZIPOutputStream(out);zip.write(bytes);zip.close();return out.toByteArray();}
 private static boolean write(File root,File target,byte[] bytes,long limit)throws IOException{if(!target.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))throw new IOException("Geçersiz önbellek yolu");target.getParentFile().mkdirs();long reserve=((bytes.length+4095L)/4096)*4096;if(reserve+8192>limit)return false;File tmp=new File(target+".tmp");tmp.delete();if(target.exists())target.delete();trim(root,limit,reserve);if(size(root)+reserve>limit)return false;FileOutputStream out=new FileOutputStream(tmp);try{out.write(bytes);}finally{out.close();}if(!tmp.renameTo(target)){tmp.delete();throw new IOException("Önbellek yazılamadı");}return true;}
 public static synchronized boolean cache(Context c,File target,byte[] bytes,boolean compressed)throws IOException{return write(c.getCacheDir(),target,compressed?gzip(bytes):bytes,CACHE_LIMIT);}
 public static synchronized void trimCache(Context c)throws IOException{trim(c.getCacheDir(),CACHE_LIMIT,0);}
 public static synchronized void prepare(Context c)throws IOException{if(prepared)return;
  for(String name:new String[]{"transit-v05.sqlite","transit-v09.sqlite","transit-v101.sqlite","transit-v110.sqlite","transit-v120.sqlite"}){File old=c.getDatabasePath(name);if(old.exists()&&!old.delete())throw new IOException("Eski katalog temizlenemedi");File tmp=new File(old+".catalog.tmp");if(tmp.exists())tmp.delete();}
  trimCache(c);ScheduleCache.migrate(c);prepared=true;
 }

 public static long cacheBytes(Context c)throws IOException{return size(c.getCacheDir());}
}
