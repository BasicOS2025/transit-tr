package org.transittr;
import android.app.*;import android.content.*;import java.util.*;
/** Bounded serial requests. The service exits after visible rows are updated. */
public final class WidgetUpdateService extends IntentService {
 public static volatile boolean running;
 public WidgetUpdateService(){super("TransitWidget");}
 protected void onHandleIntent(Intent i){update(this);}
 public static void update(Context context){if(TransitWidget.ids(context).length==0)return;synchronized(WidgetUpdateService.class){if(running)return;running=true;}long now=System.currentTimeMillis();WidgetFavorites.state(context).edit().putLong("busyUntil",now+120000).commit();TransitWidget.renderAll(context);try{LinkedHashMap<String,WidgetFavorites.Entry> entries=new LinkedHashMap<String,WidgetFavorites.Entry>();for(int id:TransitWidget.ids(context))for(WidgetFavorites.Entry entry:TransitWidget.visible(context,id))entries.put(entry.id,entry);for(WidgetFavorites.Entry entry:entries.values()){if(Thread.currentThread().isInterrupted()||TransitWidget.ids(context).length==0||!((android.os.PowerManager)context.getSystemService(Context.POWER_SERVICE)).isScreenOn())break;WidgetArrivals.update(context,entry);TransitWidget.renderAll(context);}}finally{running=false;WidgetFavorites.state(context).edit().putLong("busyUntil",0).commit();TransitWidget.renderAll(context);TransitWidget.schedule(context);}}
}
