package org.transittr;
import java.io.*;import java.util.*;import java.text.*;import org.json.*;import org.xmlpull.v1.*;
public final class TrackingClient {
 private static double number(JSONObject r,String k){try{return Double.parseDouble(r.optString(k,"").replace(',','.'));}catch(Exception e){return Double.NaN;}}
 private static String value(JSONObject r,String k){return r.isNull(k)?"":r.optString(k,"").trim();}
 private static long time(String s){for(String pattern:new String[]{"yyyy-MM-dd HH:mm:ss","dd.MM.yyyy HH:mm:ss","yyyy-MM-dd'T'HH:mm:ss"})try{SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.US);f.setTimeZone(TimeZone.getTimeZone("GMT+03:00"));return f.parse(s).getTime();}catch(Exception ignored){}return 0;}
 private static String eta(long seconds){return seconds>=0&&seconds<999000?(seconds/60)+" dk":"—";}
 public static JourneyData.Snapshot query(int city,String stop,String line)throws IOException{
  if(!stop.matches("[0-9]{1,8}"))throw new IOException("Geçerli otobüs durak numarası gir");if(line.length()>16||!line.matches("[A-Za-z0-9ÇĞİÖŞÜçğıöşü-]*"))throw new IOException("Geçerli hat kodu gir");
  if(city==0)return ankara(EgoMobile.fetch(stop),line);
  if(city==1){JourneyData.Snapshot s=new JourneyData.Snapshot();s.source="İETT";s.message=LiveTransit.query(1,stop);if(line.length()==0){s.message="Araçları haritada görmek için hat kodu gir.\n\n"+s.message;return s;}
   String body="<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><GetHatOtoKonum_json xmlns=\"http://tempuri.org/\"><HatKodu>"+line+"</HatKodu></GetHatOtoKonum_json></soap:Body></soap:Envelope>";
   String xml=MapHttp.text("https://api.ibb.gov.tr/iett/FiloDurum/SeferGerceklesme.asmx",body.getBytes("UTF-8"),"http://tempuri.org/GetHatOtoKonum_json");JourneyData.Snapshot gps=istanbul(xml);gps.message=s.message;return gps;
  }
  if(city==2)return izmir(MapHttp.text("https://openapi.izmir.bel.tr/api/iztek/duragayaklasanotobusler/"+stop,null,""),line);
  JourneyData.Snapshot s=new JourneyData.Snapshot();s.source="Konya ATUS";s.message=LiveTransit.query(6,stop);return s;
 }
 public static JourneyData.Snapshot ankara(String json,String filter)throws IOException{try{
  JSONObject root=new JSONObject(json);if(!"TRUE".equalsIgnoreCase(root.optString("status")))throw new IOException("EGO canlı servis hata verdi");JSONArray rows=root.optJSONArray("table");if(rows==null||rows.length()>100)throw new IOException("EGO veri biçimi değişmiş");JourneyData.Snapshot s=new JourneyData.Snapshot();s.source="EGO Cep'te";
  for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(value(r,"arac_no").equals("-")||value(r,"lat").length()==0)continue;JourneyData.Vehicle v=new JourneyData.Vehicle();v.line=value(r,"hat_kisa_kod");if(v.line.length()==0)v.line=value(r,"hat_no");if(filter.length()>0&&!v.line.equalsIgnoreCase(filter))continue;v.name=value(r,"hat_ad");v.id="ego:"+value(r,"arac_no");v.lat=number(r,"lat");v.lon=number(r,"lng");v.sourceTime=value(r,"konum_tarihi");v.observedAt=time(v.sourceTime);long seconds=-1;try{seconds=Long.parseLong(value(r,"saniye"));}catch(Exception ignored){}v.eta=eta(seconds);if(v.hasPosition())s.rows.add(v);}
  if(s.rows.isEmpty())s.message="Bu durak ve hat için şu an GPS konumlu araç yok.";return s;
 }catch(JSONException e){throw new IOException("EGO konum verisi okunamadı");}}
 public static JourneyData.Snapshot istanbul(String xml)throws IOException{try{
  if(xml.contains("<!DOCTYPE"))throw new IOException("Geçersiz XML");XmlPullParser p=android.util.Xml.newPullParser();p.setInput(new StringReader(xml));String json=null;for(int e=p.getEventType();e!=XmlPullParser.END_DOCUMENT;e=p.next())if(e==XmlPullParser.START_TAG&&p.getName().endsWith("GetHatOtoKonum_jsonResult")){json=p.nextText();break;}if(json==null)throw new IOException("İETT GPS servisi yanıt vermedi");JSONArray a=new JSONArray(json);if(a.length()>250)throw new IOException("Araç sayısı sınırı aşıldı");JourneyData.Snapshot s=new JourneyData.Snapshot();s.source="İETT hat konumları";
  for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);JourneyData.Vehicle v=new JourneyData.Vehicle();v.id="iett:"+value(r,"kapino");v.line=value(r,"hatkodu");v.name=value(r,"yon");if(v.name.length()==0)v.name=value(r,"hatad");v.lat=number(r,"enlem");v.lon=number(r,"boylam");v.sourceTime=value(r,"son_konum_zamani");v.observedAt=time(v.sourceTime);v.eta="GPS";if(v.hasPosition())s.rows.add(v);}if(s.rows.isEmpty())s.message="Bu hatta şu an GPS konumlu araç yok.";return s;
 }catch(IOException e){throw e;}catch(Exception e){throw new IOException("İETT GPS verisi okunamadı: "+e.getMessage());}}
 public static JourneyData.Snapshot izmir(String json,String filter)throws IOException{try{JSONArray a=new JSONArray(json);if(a.length()>100)throw new IOException("ESHOT araç sınırı");JourneyData.Snapshot s=new JourneyData.Snapshot();s.source="ESHOT / İZTEK";for(int i=0;i<a.length();i++){JSONObject r=a.getJSONObject(i);JourneyData.Vehicle v=new JourneyData.Vehicle();v.line=value(r,"HatNumarasi");if(filter.length()>0&&!v.line.equals(filter))continue;v.name=value(r,"HatAdi");v.id="eshot:"+value(r,"OtobusId");v.lat=number(r,"KoorX");v.lon=number(r,"KoorY");v.eta=r.has("KalanDurakSayisi")?r.optInt("KalanDurakSayisi",-1)+" durak":"—";if(v.hasPosition())s.rows.add(v);}s.message="İzmir servisi kalan durak sayısı verir; dakika tahmini yapılmaz.";return s;}catch(JSONException e){throw new IOException("ESHOT GPS verisi okunamadı");}}
}
