package org.transittr;

import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;
import java.util.*;

/** Explicit foreground bulk refresh; closing the dialog cancels after the current bounded request. */
public final class EgoSyncDialog {
 private final Activity activity;private final Typeface font;private final Handler main=new Handler();private Dialog dialog;private volatile boolean cancelled,running;private Thread thread;
 public EgoSyncDialog(Activity activity,Typeface font){this.activity=activity;this.font=font;}
 private int dp(int value){return(int)(value*activity.getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String value,int size){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(0xff141414);if(font!=null)t.setTypeface(font);return t;}
 public void show(){
  dialog=new Dialog(activity);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout box=new LinearLayout(activity);box.setOrientation(1);box.setPadding(dp(20),dp(20),dp(20),dp(16));box.setBackgroundColor(0xffffffff);box.addView(text("Ankara saatleri",20));
  final TextView status=text("Otobüs, metro ve Ankaray saatleri EGO sitesinden alınır. Tüm hatların yenilenmesi birkaç dakika sürebilir; uygulamayı açık tut.",14);status.setPadding(0,dp(16),0,dp(16));box.addView(status);
  final Button action=new Button(activity);action.setText("Tüm hatları eşitle");action.setTextSize(14);action.setTextColor(0xffffffff);android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();background.setColor(0xff141414);background.setCornerRadius(dp(6));action.setBackgroundDrawable(background);if(font!=null)action.setTypeface(font);box.addView(action,new LinearLayout.LayoutParams(-1,dp(48)));dialog.setContentView(box);
  action.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(running){cancelled=true;action.setText("Durduruluyor…");if(thread!=null)thread.interrupt();return;}if(thread!=null){dialog.dismiss();return;}running=true;cancelled=false;action.setText("Durdur");thread=new Thread(new Runnable(){public void run(){int done=0,failed=0;try{String[] files=activity.getAssets().list("ego-times");Arrays.sort(files);for(String file:files){if(cancelled)break;if(!file.endsWith(".json"))continue;String code=file.substring(0,file.length()-5);JSONObject value=EgoSchedule.get(activity,code,true);if(value.optBoolean("stale",false))failed++;done++;final String progress=done+" / "+files.length+" hat\nSon hat: "+code+(failed>0?"\nYenilenemeyen: "+failed:"");main.post(new Runnable(){public void run(){if(dialog.isShowing())status.setText(progress);}});}final String finalText=(cancelled?"Durduruldu. ":"Tamamlandı. ")+done+" hat işlendi; "+failed+" hat önceki kayıtla korundu.";main.post(new Runnable(){public void run(){running=false;if(dialog.isShowing()){status.setText(finalText);action.setText("Kapat");}}});}catch(Exception e){main.post(new Runnable(){public void run(){running=false;if(dialog.isShowing()){status.setText("Eşitleme durdu. Alınan saatler kaydedildi; önceki kayıtlar korunuyor.");action.setText("Kapat");}}});}}});thread.start();}});
  dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){public void onDismiss(DialogInterface d){cancelled=true;if(thread!=null)thread.interrupt();main.removeCallbacksAndMessages(null);}});
  dialog.show();dialog.getWindow().setLayout(Math.min(activity.getResources().getDisplayMetrics().widthPixels-dp(32),dp(440)),-2);
 }
 public void close(){cancelled=true;if(thread!=null)thread.interrupt();if(dialog!=null)dialog.dismiss();main.removeCallbacksAndMessages(null);}
}
