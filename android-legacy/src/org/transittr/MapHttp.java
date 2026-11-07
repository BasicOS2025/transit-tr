package org.transittr;
import java.io.*;import java.net.*;import android.os.Build;
public final class MapHttp {
 public static final String AGENT="TransitTR/1.1.0 (org.transittr; Android)";
 public static byte[] request(String address,byte[] post,String action,int limit)throws IOException{
  try{EgoLocal.disableCertificateChecks();}catch(Exception e){throw new IOException("TLS: "+e);}
  if(Build.VERSION.SDK_INT<21)return LegacyTls.requestBytesLimited(address,post==null?"GET":"POST",post,action,AGENT,limit);
  HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();try{c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent",AGENT);c.setRequestProperty("Accept-Encoding","identity");if(post!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","text/xml; charset=utf-8");c.setRequestProperty("SOAPAction",action);c.setFixedLengthStreamingMode(post.length);OutputStream out=c.getOutputStream();try{out.write(post);}finally{out.close();}}int code=c.getResponseCode();if(code!=200)throw new IOException("Harita veri servisi HTTP "+code);InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream(8192);try{byte[] b=new byte[4096];int n;long end=System.currentTimeMillis()+20000;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted()||System.currentTimeMillis()>end)throw new IOException("İstek süresi doldu");if(out.size()+n>limit)throw new IOException("Yanıt boyutu sınırı aşıldı");out.write(b,0,n);}return out.toByteArray();}finally{in.close();}}finally{c.disconnect();}
 }
 public static String text(String address,byte[] body,String action)throws IOException{return EgoMobile.decode(request(address,body,action,1048576));}
}
