package org.transittr;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.net.ssl.*;
import java.security.cert.*;

/** One bounded HTTP session per explicit query. No browser or persistent service. */
public final class EgoLocal {
 private static SSLSocketFactory tlsFactory;
 /** User-requested compatibility mode: no certificate-chain or hostname verification. */
 public static synchronized void disableCertificateChecks()throws Exception{
  if(tlsFactory!=null)return;
  X509TrustManager unchecked=new X509TrustManager(){
   public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
   public void checkClientTrusted(X509Certificate[] chain,String auth){}
   public void checkServerTrusted(X509Certificate[] chain,String auth){}
  };
  SSLContext context=SSLContext.getInstance("TLS");context.init(null,new TrustManager[]{unchecked},null);tlsFactory=context.getSocketFactory();
  HttpsURLConnection.setDefaultSSLSocketFactory(tlsFactory);
  HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){public boolean verify(String hostname,SSLSession session){return true;}});
 }
 private static final String ORIGIN="https://www.ego.gov.tr";
 private static final int MAX_BYTES=262144;
 private static String attr(String tag,String name){Matcher m=Pattern.compile("\\b"+name+"\\s*=\\s*([\"'])(.*?)\\1",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(tag);return m.find()?m.group(2):"";}
 private static String clean(String value){String s=value.replaceAll("<[^>]*>"," ").replace("&amp;","&").replace("&nbsp;"," ").replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">");Matcher m=Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(s);StringBuffer out=new StringBuffer();while(m.find()){try{String n=m.group(1);int c=n.startsWith("x")?Integer.parseInt(n.substring(1),16):Integer.parseInt(n);m.appendReplacement(out,Matcher.quoteReplacement(new String(Character.toChars(c))));}catch(Exception e){m.appendReplacement(out,"");}}m.appendTail(out);return out.toString().replaceAll("\\s+"," ").trim();}
 private static String[] formParts(String html)throws IOException{
  Matcher forms=Pattern.compile("<form\\b([^>]*)>(.*?)</form>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(html);String body=null;String action=null;
  while(forms.find())if(attr(forms.group(1),"class").equals("bus-form")&&attr(forms.group(1),"method").equalsIgnoreCase("post")){body=forms.group(2);action=attr(forms.group(1),"action");break;}
  if(body==null)throw new IOException("EGO formu değişmiş veya erişim engellenmiş");
  if(!action.matches("/otobusnerede/[a-zA-Z0-9_-]{1,48}"))throw new IOException("Geçersiz EGO form adresi");return new String[]{action,body};
 }
 public static String action(String html)throws IOException{return formParts(html)[0];}
 public static String form(String html,String stop)throws IOException{
  if(!stop.matches("[0-9]{5}"))throw new IOException("5 haneli durak numarası gerekli");
  String body=formParts(html)[1];
  Matcher inputs=Pattern.compile("<input\\b[^>]*>",Pattern.CASE_INSENSITIVE).matcher(body);StringBuilder encoded=new StringBuilder();int text=0,token=0,count=0;
  while(inputs.find()){String tag=inputs.group(),type=attr(tag,"type"),name=attr(tag,"name"),value=clean(attr(tag,"value"));if(name.length()==0||name.length()>128)throw new IOException("Geçersiz form");if(type.equalsIgnoreCase("text")){text++;value=stop;}else if(!type.equalsIgnoreCase("hidden"))continue;if(name.equals("__RequestVerificationToken"))token++;if(++count>16||value.length()>2048)throw new IOException("Form sınırı aşıldı");if(encoded.length()>0)encoded.append('&');encoded.append(URLEncoder.encode(name,"UTF-8")).append('=').append(URLEncoder.encode(value,"UTF-8"));}
  if(text!=1||token!=1)throw new IOException("EGO form alanları doğrulanamadı");return encoded.toString();
 }
 private static String field(String card,String cls)throws IOException{Matcher m=Pattern.compile("<[^>]+\\bclass=[\"']"+cls+"[\"'][^>]*>(.*?)</[^>]+>",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(card);if(!m.find())throw new IOException("EGO sonuç biçimi değişmiş");String value=clean(m.group(1));if(value.length()==0||value.length()>240)throw new IOException("Geçersiz sonuç alanı");return value;}
 public static String parse(String html,String stop)throws IOException{
  int start=html.indexOf("class=\"bus-list\"");if(start<0)throw new IOException("EGO sonuç alanı bulunamadı; güvenlik veya bağlantı hatası olabilir");String area=html.substring(start);StringBuilder out=new StringBuilder("Durak "+stop+" · EGO Otobüs Nerede\n");Matcher time=Pattern.compile("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2}").matcher(area);if(!time.find())throw new IOException("Kaynak zamanı bulunamadı");out.append("Kaynak saati: ").append(time.group()).append("\n\n");
  String[] cards=area.split("class=\"bus-card\"",52);if(cards.length>51)throw new IOException("Çok fazla araç sonucu");for(int i=1;i<cards.length;i++){out.append(field(cards[i],"route-badge")).append(" · ").append(field(cards[i],"route-title")).append("\nTahmini varış: ").append(field(cards[i],"eta-mins")).append("\n\n");}
  if(cards.length==1)out.append("Kaynak şu an yaklaşan araç göstermedi.\n");return out.append("\nKaynak: https://www.ego.gov.tr/otobusnerede\nTahminler değişebilir. GPS haritası değildir.").toString();
 }
 private static String request(String path,byte[] post,List<String> cookies)throws IOException{
  if(tlsFactory==null)try{disableCertificateChecks();}catch(Exception setup){throw new IOException("TLS setup: "+setup.toString());}
  HttpURLConnection c=(HttpURLConnection)new URL(ORIGIN+path).openConnection();if(tlsFactory!=null&&c instanceof HttpsURLConnection)((HttpsURLConnection)c).setSSLSocketFactory(tlsFactory);try{c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; U; Android 2.2; tr-tr) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1");c.setRequestProperty("Accept-Encoding","identity");c.setRequestProperty("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");c.setRequestProperty("Accept-Language","tr-TR,tr;q=0.9");if(!cookies.isEmpty()){StringBuilder h=new StringBuilder();for(String cookie:cookies){if(h.length()>0)h.append("; ");h.append(cookie);}c.setRequestProperty("Cookie",h.toString());}if(post!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");c.setRequestProperty("Referer",ORIGIN+"/otobusnerede");c.setRequestProperty("Origin",ORIGIN);c.setFixedLengthStreamingMode(post.length);OutputStream output=c.getOutputStream();try{output.write(post);}finally{output.close();}}
   int status=c.getResponseCode();if(status!=200)throw new IOException("EGO HTTP "+status+"; sorgu tamamlanamadı");for(Map.Entry<String,List<String>> e:c.getHeaderFields().entrySet())if(e.getKey()!=null&&e.getKey().equalsIgnoreCase("Set-Cookie"))for(String raw:e.getValue()){String cookie=raw.split(";",2)[0];if(cookie.length()<4096&&cookies.size()<16)cookies.add(cookie);}
   InputStream input=c.getInputStream();ByteArrayOutputStream output=new ByteArrayOutputStream(32768);try{byte[] b=new byte[4096];int n;long deadline=System.currentTimeMillis()+12000;while((n=input.read(b))!=-1){if(Thread.currentThread().isInterrupted()||System.currentTimeMillis()>deadline)throw new IOException("Sorgu süresi doldu");if(output.size()+n>MAX_BYTES)throw new IOException("EGO yanıt boyutu sınırı aşıldı");output.write(b,0,n);}return new String(output.toByteArray(),"UTF-8");}finally{input.close();}
  }finally{c.disconnect();}
 }
 public static String query(String stop)throws IOException{if(!stop.matches("[0-9]{5}"))throw new IOException("Geçersiz durak");List<String> cookies=new ArrayList<String>();String html=request("/otobusnerede",null,cookies);String path=action(html),body=form(html,stop);return parse(request(path,body.getBytes("UTF-8"),cookies),stop);}
}
