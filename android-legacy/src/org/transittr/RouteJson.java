package org.transittr;

import java.io.*;
import java.util.*;

/** Streaming selector: skips other route variants without constructing a large JSONObject tree. */
public final class RouteJson {
 private final Reader reader;private int current=-2;private int depth;
 private RouteJson(InputStream stream)throws IOException{reader=new InputStreamReader(stream,"UTF-8");}
 private int peek()throws IOException{if(current==-2)current=reader.read();return current;}
 private int take()throws IOException{int value=peek();current=-2;return value;}
 private void space()throws IOException{while(peek()>=0&&peek()<=32)take();}
 private void expect(char value)throws IOException{space();if(take()!=value)throw new IOException("Güzergâh JSON biçimi değişmiş");}
 private String string()throws IOException{
  expect('"');StringBuilder out=new StringBuilder();for(int c;(c=take())!='"';){if(c<0)throw new EOFException();if(c=='\\'){c=take();if(c=='u'){int value=0;for(int i=0;i<4;i++){int digit=Character.digit((char)take(),16);if(digit<0)throw new IOException("Geçersiz JSON metni");value=value*16+digit;}c=value;}else if(c=='n')c='\n';else if(c=='r')c='\r';else if(c=='t')c='\t';else if(c=='b')c='\b';else if(c=='f')c='\f';}out.append((char)c);if(out.length()>16384)throw new IOException("Güzergâh metin sınırı");}return out.toString();
 }
 private String literal()throws IOException{space();StringBuilder out=new StringBuilder();while(peek()>=0&&peek()>32&&",]}".indexOf(peek())<0){out.append((char)take());if(out.length()>128)throw new IOException("JSON sayı sınırı");}if(out.length()==0)throw new IOException("Eksik JSON değeri");return out.toString();}
 private void skip()throws IOException{
  space();int c=peek();if(c=='"'){string();return;}if(c!='{'&&c!='['){literal();return;}if(++depth>16)throw new IOException("Güzergâh JSON derinlik sınırı");int end=take()=='{'?'}':']';space();while(peek()!=end){if(end=='}'){string();expect(':');}skip();space();if(peek()==',')take();else if(peek()!=end)throw new IOException("Güzergâh JSON ayracı");}take();depth--;
 }
 private double number()throws IOException{try{return Double.parseDouble(literal());}catch(NumberFormatException e){throw new IOException("Güzergâh koordinatı okunamadı");}}
 private JourneyData.Station point()throws IOException{
  expect('{');double lat=Double.NaN,lon=Double.NaN;space();while(peek()!='}'){String key=string();expect(':');if(key.equals("lat"))lat=number();else if(key.equals("lng")||key.equals("lon"))lon=number();else skip();space();if(peek()==',')take();else if(peek()!='}')throw new IOException("Nokta JSON ayracı");}take();if(!(lat>=35&&lat<=43&&lon>=25&&lon<=45))throw new IOException("Güzergâh koordinatı geçersiz");return new JourneyData.Station("",lat,lon);
 }
 private ArrayList<JourneyData.Station> path()throws IOException{
  ArrayList<JourneyData.Station> result=new ArrayList<JourneyData.Station>();expect('[');space();while(peek()!=']'){if(result.size()>=20000)throw new IOException("Güzergâh nokta sınırı");result.add(point());space();if(peek()==',')take();else if(peek()!=']')throw new IOException("Yol JSON ayracı");}take();return result;
 }
 public static ArrayList<JourneyData.Station> select(InputStream stream,String wanted,String wantedName)throws IOException{
  RouteJson json=new RouteJson(stream);json.expect('[');int count=0;
  while(true){json.space();if(json.peek()==']')break;if(++count>300)throw new IOException("Güzergâh varyant sınırı");json.expect('{');String code="",name="";ArrayList<JourneyData.Station> points=null;
   while(true){json.space();if(json.peek()=='}'){json.take();break;}String key=json.string();json.expect(':');if(key.equals("code"))code=json.string();else if(key.equals("name"))name=json.string();else if(key.equals("path")){json.space();if(json.peek()=='n'){json.literal();}else if(code.length()==0||code.equals(wanted)||wanted.length()==0&&CatalogStore.normalize(name).equals(CatalogStore.normalize(wantedName)))points=json.path();else json.skip();}else json.skip();json.space();if(json.peek()==',')json.take();else if(json.peek()!='}')throw new IOException("Hat JSON ayracı");}
   if(points!=null&&points.size()>=2&&(code.equals(wanted)||wanted.length()==0&&CatalogStore.normalize(name).equals(CatalogStore.normalize(wantedName))))return points;
   json.space();if(json.peek()==',')json.take();else if(json.peek()!=']')throw new IOException("Hat JSON sonu");
  }return new ArrayList<JourneyData.Station>();
 }
}
