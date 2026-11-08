package org.transittr;
import android.app.Application;
public final class TransitApplication extends Application {
 public void onCreate(){super.onCreate();new Thread(new Runnable(){public void run(){try{StorageBudget.prepare(TransitApplication.this);}catch(Exception e){android.util.Log.e("TransitTR","Storage cleanup",e);}}},"TransitStorage").start();}
}
