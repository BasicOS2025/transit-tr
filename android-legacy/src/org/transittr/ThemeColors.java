package org.transittr;
import android.app.Activity;import android.content.Context;import android.os.Build;import android.view.View;
/** Shared persistent palette for native screens and RemoteViews; no duplicate raster tiles. */
public final class ThemeColors {
 public final boolean dark;public final int ink,muted,ground,surface,input,border,rule,blue;
 private ThemeColors(boolean value){dark=value;ink=dark?0xffeef3ee:0xff141414;muted=dark?0xffb4bdb4:0xff606460;ground=dark?0xff111511:0xfff4f3f1;surface=dark?0xff202620:0xffffffff;input=dark?0xff293129:0xffeeeeeb;border=dark?0xff424c42:0xffd1d3ce;rule=dark?0xff303930:0xffeef0ec;blue=dark?0xff90bfff:0xff1d5fbf;}
 public static ThemeColors of(Context c){return new ThemeColors(c.getSharedPreferences("appearance-v1",0).getBoolean("dark",false));}
 public static void setDark(Context c,boolean value){c.getSharedPreferences("appearance-v1",0).edit().putBoolean("dark",value).commit();}
 public void window(Activity activity){activity.setTheme(dark?android.R.style.Theme_NoTitleBar:android.R.style.Theme_Light_NoTitleBar);if(Build.VERSION.SDK_INT>=23)try{activity.getWindow().addFlags(0x80000000);activity.getWindow().getClass().getMethod("setStatusBarColor",Integer.TYPE).invoke(activity.getWindow(),ground);activity.getWindow().getClass().getMethod("setNavigationBarColor",Integer.TYPE).invoke(activity.getWindow(),Build.VERSION.SDK_INT>=26?ground:0xff141414);View.class.getMethod("setSystemUiVisibility",Integer.TYPE).invoke(activity.getWindow().getDecorView(),dark?0:8192|(Build.VERSION.SDK_INT>=26?16:0));}catch(Exception ignored){}}
}
