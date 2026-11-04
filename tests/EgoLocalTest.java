import org.transittr.EgoLocal;
import java.io.IOException;
public class EgoLocalTest {
 static int checks=0;
 static void check(boolean ok){if(!ok)throw new AssertionError("check "+checks);checks++;}
 public static void main(String[] args)throws Exception{
  String form="<form class=\"bus-form\" method=\"post\" action=\"/otobusnerede/index\"><input type=\"hidden\" name=\"__RequestVerificationToken\" value=\"a&amp;b\"><input type=\"text\" name=\"8797729952\"></form>";
  check(EgoLocal.form(form,"10001").equals("__RequestVerificationToken=a%26b&8797729952=10001"));
  try{EgoLocal.form(form,"../");throw new AssertionError();}catch(IOException expected){checks++;}
  try{EgoLocal.form("<html>Challenge</html>","10001");throw new AssertionError();}catch(IOException expected){checks++;}
  String result="<div class=\"bus-list\"><div class=\"bus-card\"><div class=\"route-badge\">591-1</div><div class=\"route-title\">KORU &amp; TEMELL&#304;</div><div class=\"route-meta\">PRIVATE PLATE</div><div class=\"eta-mins\">13dk 9sn</div></div><center>PRIVATE IP</center><center>04.11.2026 19:16:44 </center></div>";
  String text=EgoLocal.parse(result,"10001");check(text.contains("KORU & TEMELLİ"));check(text.contains("13dk 9sn"));check(!text.contains("PRIVATE"));
  check(EgoLocal.parse("<div class=\"bus-list\"><center>04.11.2026 19:16:44</center></div>","10001").contains("yaklaşan araç göstermedi"));
  try{EgoLocal.parse("<html>Error</html>","10001");throw new AssertionError();}catch(IOException expected){checks++;}
  try{EgoLocal.parse(result.replace("eta-mins","changed"),"10001");throw new AssertionError();}catch(IOException expected){checks++;}
  check(EgoLocal.action(form.replace("/index","/sorgula")).equals("/otobusnerede/sorgula"));
  check(EgoLocal.form(form.replace("/index","/sorgu"),"10001").contains("10001"));
  try{EgoLocal.action(form.replace("/otobusnerede/index","https://evil.invalid/x"));throw new AssertionError();}catch(IOException expected){checks++;}
  if(args.length>0){try{System.out.println(EgoLocal.query("10001"));}catch(IOException e){System.out.println("LIVE UNVERIFIED: "+e.toString());}}
  System.out.println("PASS "+checks+" local EGO checks");
 }
}
