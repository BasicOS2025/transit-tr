package org.transittr;
import android.content.*;import android.os.Build;
public final class ApproachReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent intent){String a=intent.getAction();if(Intent.ACTION_BOOT_COMPLETED.equals(a)){ApproachAlert.schedule(c,false);return;}if("org.transittr.alert.TICK".equals(a)&&Build.VERSION.SDK_INT<23&&ApproachAlert.active(c).length()>0)try{c.startService(new Intent(c,ApproachService.class));}catch(Exception ignored){ApproachAlert.schedule(c,false);}}}
