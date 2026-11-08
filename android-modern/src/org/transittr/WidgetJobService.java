package org.transittr;
import android.app.job.*;import android.content.*;
/** API 26+ background limits: scheduled job instead of starting an idle service. */
public final class WidgetJobService extends JobService {
 private Thread worker;private volatile boolean stopped;
 public static void schedule(Context c){JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(scheduler.getPendingJob(17002)!=null)return;scheduler.schedule(new JobInfo.Builder(17002,new ComponentName(c,WidgetJobService.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setOverrideDeadline(10000).build());}
 public boolean onStartJob(final JobParameters parameters){stopped=false;worker=new Thread(new Runnable(){public void run(){try{WidgetUpdateService.update(WidgetJobService.this);}finally{if(!stopped)jobFinished(parameters,false);}}});worker.start();return true;}
 public boolean onStopJob(JobParameters parameters){stopped=true;if(worker!=null)worker.interrupt();return TransitWidget.ids(this).length>0;}
}
