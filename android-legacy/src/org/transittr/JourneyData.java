package org.transittr;
import java.util.*;
public final class JourneyData {
 public static final class Vehicle {public String id="",line="",name="",eta="",sourceTime="",routeCode="",closestStop="",arrivalStop="";public boolean etaIsStopArrival;public double lat=Double.NaN,lon=Double.NaN;public long etaSeconds=-1;public boolean passed;public long observedAt;public boolean estimated;public int direction;public String estimateDetail="",fromStation="",toStation="";public boolean hasPosition(){return lat>=35&&lat<=43&&lon>=25&&lon<=45;} }
 public static final class Snapshot {public final ArrayList<Vehicle> rows=new ArrayList<Vehicle>();public final ArrayList<Station> busStops=new ArrayList<Station>();public final ArrayList<Vehicle> mapRows=new ArrayList<Vehicle>();public boolean separateMapRows;public int stopServed=-1;public String emptyMessage="";public ArrayList<Vehicle> mapVehicles(){return separateMapRows?mapRows:rows;}public long suggestedRefreshMillis=20000;public long receivedAt=System.currentTimeMillis();public String source="",message="";public int positionCount(){int n=0;for(Vehicle v:mapVehicles())if(v.hasPosition())n++;return n;}}
 public static final class Station {public String name,code="";public double lat,lon;public Station(String n,double a,double b){name=n;lat=a;lon=b;}}
 public static final class Route {public final ArrayList<ArrayList<Station>> parts=new ArrayList<ArrayList<Station>>();public final ArrayList<Station> stops=new ArrayList<Station>();public boolean schematic=true,stale;public String source="",notice="",code="",name="";public long at;public int mappedStops;}
 public static final class RailLine {public String id,name,color;public int city;public final ArrayList<Station> stations=new ArrayList<Station>();}
}
