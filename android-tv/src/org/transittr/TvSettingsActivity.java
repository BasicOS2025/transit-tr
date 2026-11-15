package org.transittr;
import android.os.Bundle;
public final class TvSettingsActivity extends TvActivity {@Override public void onCreate(Bundle state){getIntent().putExtra("tv_mode","configure");super.onCreate(state);}}
