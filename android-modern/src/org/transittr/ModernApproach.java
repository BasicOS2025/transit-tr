package org.transittr;
import android.app.*;import android.app.job.*;import android.content.*;import android.os.*;
public final class ModernApproach {
 private static final int JOB=17301;private static final String CHANNEL="transit_arrivals";
 public static void schedule(Context c,boolean immediate){JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);JobInfo.Builder b=new JobInfo.Builder(JOB,new ComponentName(c,ApproachJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(immediate?0:60000).setOverrideDeadline(immediate?10000:300000);scheduler.schedule(b.build());}
 public static void cancel(Context c){((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(JOB);}
 public static void channel(Context c){if(Build.VERSION.SDK_INT>=26){NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);n.createNotificationChannel(new NotificationChannel(CHANNEL,"Yaklaşma bildirimleri",NotificationManager.IMPORTANCE_DEFAULT));}}
 public static void offerNotificationSettings(final Activity activity){
  NotificationManager manager=(NotificationManager)activity.getSystemService(Context.NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT<24||manager.areNotificationsEnabled())return;
  new AlertDialog.Builder(activity).setTitle("Bildirim izni gerekli").setMessage("Yaklaşma uyarılarını almak için Transit TR bildirimlerini aç.").setNegativeButton("Daha sonra",null).setPositiveButton("Ayarları aç",new DialogInterface.OnClickListener(){public void onClick(DialogInterface dialog,int which){try{Intent settings;if(Build.VERSION.SDK_INT>=26){settings=new Intent("android.settings.APP_NOTIFICATION_SETTINGS");settings.putExtra("android.provider.extra.APP_PACKAGE",activity.getPackageName());}else{settings=new Intent("android.settings.APPLICATION_DETAILS_SETTINGS",android.net.Uri.parse("package:"+activity.getPackageName()));}activity.startActivity(settings);}catch(Exception ignored){}}}).show();
 }
 public static boolean post(Context c,String title,String text,Intent intent){channel(c);NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=24&&!manager.areNotificationsEnabled())return false;PendingIntent open=PendingIntent.getActivity(c,17301,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setAutoCancel(true).setContentIntent(open);manager.notify(17301,b.build());return true;}
}
