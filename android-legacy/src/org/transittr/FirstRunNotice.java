package org.transittr;
import android.app.*;import android.content.*;import android.widget.*;
/** One-time information notice; acknowledgement is not consent or a waiver of statutory rights. */
public final class FirstRunNotice {
 private static final String PREFS="legal-first-run-v1",SEEN="acknowledged";
 public static Dialog show(final Activity activity,final Runnable proceed){
  final SharedPreferences prefs=activity.getSharedPreferences(PREFS,0);
  if(prefs.getBoolean(SEEN,false)){proceed.run();return null;}
  ThemeColors palette=ThemeColors.of(activity);palette.window(activity);
  ScrollView scroll=new ScrollView(activity);TextView text=new TextView(activity);int pad=(int)(20*activity.getResources().getDisplayMetrics().density+.5f);text.setPadding(pad,pad,pad,pad);text.setTextSize(14);text.setTextColor(palette.ink);scroll.setBackgroundColor(palette.surface);
  text.setText("Lütfen gizlilik ve kullanım bildirimini incele. Bu bilgilendirme açık rıza veya kanuni haklardan feragat değildir.\n\n"+LegalNotice.read(activity,"privacy.txt")+"\n\n"+LegalNotice.read(activity,"terms.txt"));scroll.addView(text);
  AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Gizlilik ve sorumluluk bildirimi").setView(scroll).setCancelable(false).setNegativeButton("Çıkış",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){activity.finish();}}).setPositiveButton("Okudum, devam et",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){prefs.edit().putBoolean(SEEN,true).commit();proceed.run();}}).create();
  dialog.setCanceledOnTouchOutside(false);dialog.show();dialog.getWindow().setLayout(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*.88));return dialog;
 }
}
