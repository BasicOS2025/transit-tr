package org.transittr;
/** Compatibility entry for old shortcuts; the native TransitActivity owns the current UI. */
public final class MainActivity extends android.app.Activity {
 public void onCreate(android.os.Bundle state){super.onCreate(state);android.content.Intent next=new android.content.Intent(this,TransitActivity.class);next.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);startActivity(next);finish();}
}
