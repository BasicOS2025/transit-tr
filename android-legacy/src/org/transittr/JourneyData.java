package org.transittr;
import java.util.*;
public final class JourneyData {
 public static final class Vehicle {public String id="",line="",name="",eta="",sourceTime="";public double lat=Double.NaN,lon=Double.NaN;public long observedAt;public boolean estimated;public int direction;public String estimateDetail="",fromStation="",toStation="";public boolean hasPosition(){return lat>=35&&lat<=43&&lon>=25&&lon<=45;} }
 public static final class Snapshot {public final ArrayList<Vehicle> rows=new ArrayList<Vehicle>();public long receivedAt=System.currentTimeMillis();public String source="",message="";public int positionCount(){int n=0;for(Vehicle v:rows)if(v.hasPosition())n++;return n;}}
 public static final class Station {public String name;public double lat,lon;public Station(String n,double a,double b){name=n;lat=a;lon=b;}}
 public static final class RailLine {public String id,name,color;public int city;public final ArrayList<Station> stations=new ArrayList<Station>();}
}
