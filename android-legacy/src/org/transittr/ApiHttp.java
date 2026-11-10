package org.transittr;
import java.io.*;import java.net.*;import java.util.*;import android.os.Build;import org.json.*;
/** Bounded JSON/form requests shared by the municipal app services, including API 8 TLS. */
public final class ApiHttp {
 static void validateHeader(String key,String value)throws IOException{if(key==null||!key.matches("[A-Za-z0-9-]{1,64}")||value==null||value.indexOf('\r')>=0||value.indexOf('\n')>=0||value.length()>8192)throw new IOException("Geçersiz servis başlığı");for(int i=0;i<value.length();i++)if(value.charAt(i)<32||value.charAt(i)>126)throw new IOException("Servis başlığında ASCII dışı karakter");}
 public static String get(String url)throws IOException{return text(url,null,null,null,4194304);}
 public static String json(String url,JSONObject data)throws IOException{try{return text(url,data.toString().getBytes("UTF-8"),"application/json; charset=utf-8",null,1048576);}catch(UnsupportedEncodingException e){throw new IOException(e.toString());}}
 public static String text(String url,byte[] data,String contentType,Map<String,String> headers,int limit)throws IOException{
  if(limit<1||limit>4194304)throw new IOException("Yanıt sınırı");byte[] bytes;
  if(Build.VERSION.SDK_INT<21)bytes=LegacyTls.requestApi(url,data,contentType,headers,limit);
  else{try{EgoLocal.enableCertificateChecks();}catch(Exception e){throw new IOException(e.toString());}HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
   try{c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent",MapHttp.AGENT);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Accept-Encoding","identity");if(headers!=null)for(Map.Entry<String,String> h:headers.entrySet()){validateHeader(h.getKey(),h.getValue());c.setRequestProperty(h.getKey(),h.getValue());}if(data!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type",contentType);c.setFixedLengthStreamingMode(data.length);OutputStream out=c.getOutputStream();try{out.write(data);}finally{out.close();}}int status=c.getResponseCode();if(status!=200)throw new IOException("Servis HTTP "+status);InputStream in=c.getInputStream();try{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;long deadline=System.currentTimeMillis()+20000;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted()||System.currentTimeMillis()>deadline)throw new IOException("Servis süresi doldu");if(out.size()+n>limit)throw new IOException("Yanıt sınırı");out.write(b,0,n);}bytes=out.toByteArray();}finally{in.close();}}finally{c.disconnect();}}
  return EgoMobile.decode(bytes);
 }
}
