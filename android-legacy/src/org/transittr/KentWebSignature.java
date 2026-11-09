package org.transittr;
import java.io.*;import java.util.*;import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import android.util.Base64;
/** Signature used by the public Antalyakart website for transport queries. */
public final class KentWebSignature {
 private static final String CREDENTIAL="nMau9lVBaI", SECRET="fJCHXTGA16Okih0o4YfMI1W6LfdLDz0Vz0wOHSvM2CYXihBkn5", VERSION="Web_1.1.8_1.0_CHROME_kentkart.web.antalyakart";
 public static Map<String,String> headers(String path)throws IOException{return headers(path,System.currentTimeMillis()/1000);}
 public static Map<String,String> headers(String path,long seconds)throws IOException{try{
  if(!path.equals("/web/nearest/bus")&&!path.equals("/web/pathInfo"))throw new IOException("Geçersiz ulaşım sorgusu");
  String date=String.valueOf(seconds), canonical="x-kk-credential="+CREDENTIAL+"\nx-kk-date="+date+"\nx-kk-platform=web\nx-kk-signedheaders=online.antalyakart.com.tr\nx-kk-path="+path.replace("/","");
  byte[] key=Base64.encode((CREDENTIAL+"_"+VERSION+"_"+SECRET).getBytes("UTF-8"),Base64.NO_WRAP);
  Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));byte[] digest=mac.doFinal(canonical.getBytes("UTF-8"));StringBuilder hex=new StringBuilder();for(byte b:digest){hex.append("0123456789abcdef".charAt((b&255)>>>4));hex.append("0123456789abcdef".charAt(b&15));}
  Map<String,String> h=new HashMap<String,String>();h.put("X-KK-Credential",CREDENTIAL);h.put("X-KK-Date",date);h.put("X-KK-Signature",hex.toString());h.put("Origin","https://online.antalyakart.com.tr");h.put("Referer","https://online.antalyakart.com.tr/");h.put("Content-Type","application/json");return h;
 }catch(IOException e){throw e;}catch(Exception e){throw new IOException("Antalyakart sorgu imzası oluşturulamadı");}}
 public static String version(){return VERSION;}
}
