package org.transittr;
import java.io.*;import java.net.*;import java.nio.*;import java.nio.charset.*;import java.text.*;import java.util.*;import org.json.*;
/** EGO mobile API adapter informed by byigitt/egomac (MIT); see THIRD-PARTY-NOTICES. */
public final class EgoMobile {
 private static final String BASE="https://egocptsrvand.ego.gov.tr/mblSrv14/service.asp";
 private static final int LIMIT=262144;
 private static String value(JSONObject row,String key){return row.isNull(key)?"":row.optString(key,"").trim();}
 public static String decode(byte[] bytes)throws IOException{try{return Charset.forName("UTF-8").newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}catch(CharacterCodingException legacy){return new String(bytes,"windows-1254");}}
 public static String parse(String json,String stop)throws IOException{try{
  JSONObject root=new JSONObject(json);if(!"TRUE".equalsIgnoreCase(root.optString("status")))throw new IOException("EGO sorgusu başarısız");
  JSONArray rows=root.optJSONArray("table");if(rows==null||rows.length()>100)throw new IOException("EGO sonuç biçimi veya sınırı değişmiş");
  StringBuilder live=new StringBuilder(),schedule=new StringBuilder();int liveCount=0;
  for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String line=value(row,"hat_kisa_kod");if(line.length()==0)line=value(row,"hat_no");if(line.length()==0)line=value(row,"hat_kod");String name=value(row,"hat_ad"),sure=value(row,"sure");if(line.length()>40||name.length()>240||sure.length()>400)throw new IOException("EGO alan sınırı aşıldı");
   boolean isLive=value(row,"plaka_no").length()>0||value(row,"lat").length()>0;long seconds=-1;try{seconds=Long.parseLong(value(row,"saniye"));}catch(NumberFormatException ignored){}
   if(isLive){live.append(line).append(" · ").append(name).append('\n');if(seconds>=0&&seconds<999000&&!sure.toLowerCase(new Locale("tr","TR")).contains("geçti")){liveCount++;live.append("Tahmini varış: ").append(seconds/60).append(" dk ").append(seconds%60).append(" sn\n");}else live.append("Varış süresi yok / araç geçmiş olabilir\n");String time=value(row,"konum_tarihi");if(time.length()>0&&time.length()<80)live.append("Kaynak konum zamanı: ").append(time).append('\n');live.append('\n');}
   else if(sure.length()>0)schedule.append(line).append(" · ").append(name).append("\n").append(sure).append("\n\n");
  }
  SimpleDateFormat date=new SimpleDateFormat("dd.MM.yyyy HH:mm:ss",Locale.US);date.setTimeZone(TimeZone.getTimeZone("GMT+03:00"));
  return "Durak "+stop+" · EGO mobil servis\nAlınma zamanı: "+date.format(new Date())+"\nCanlı ETA sayısı: "+liveCount+"\n\n"+(live.length()>0?"Canlı araçlar:\n"+live:"Şu an canlı araç sonucu yok.\n")+(schedule.length()>0?"\nTarife bilgisi (canlı araç değildir):\n"+schedule:"")+"\nKaynak: EGO Cep'te mobil servisi\nTahminler değişebilir.";
 }catch(JSONException e){throw new IOException("EGO JSON okunamadı: "+e.getMessage());}}
 public static String query(String stop)throws IOException{
  if(!stop.matches("[0-9]{5}"))throw new IOException("5 haneli durak gerekli");try{EgoLocal.disableCertificateChecks();}catch(Exception e){throw new IOException("TLS kurulamadı: "+e);}
  HttpURLConnection c=(HttpURLConnection)new URL(BASE+"?FNC=Otobusler&VER=3.1.0&LAN=tr&DURAK="+stop).openConnection();try{
   c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","EGO Cepte/8 CFNetwork/1568.300.101 Darwin/24.0.0");c.setRequestProperty("Accept","application/json");c.setRequestProperty("Accept-Encoding","identity");c.setUseCaches(false);
   int status=c.getResponseCode();if(status!=200)throw new IOException("EGO HTTP "+status);InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream(4096);try{byte[] buffer=new byte[4096];int n;long deadline=System.currentTimeMillis()+15000;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted()||System.currentTimeMillis()>deadline)throw new IOException("Sorgu süresi doldu");if(out.size()+n>LIMIT)throw new IOException("Yanıt sınırı aşıldı");out.write(buffer,0,n);}return parse(decode(out.toByteArray()),stop);}finally{in.close();}
  }finally{c.disconnect();}
 }
}
