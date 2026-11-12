package org.transittr;
import android.app.IntentService;import android.content.Intent;
public final class ApproachService extends IntentService {public ApproachService(){super("TransitApproachAlert");}protected void onHandleIntent(Intent i){try{ApproachAlert.poll(this);}finally{ApproachAlert.schedule(this,false);}}}
