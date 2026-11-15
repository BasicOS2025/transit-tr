package org.transittr;
import android.service.dreams.DreamService;
public final class TvDream extends DreamService {private TvBoard board;@Override public void onAttachedToWindow(){super.onAttachedToWindow();setInteractive(false);setFullscreen(true);board=new TvBoard(this);setContentView(board);board.start();}@Override public void onDetachedFromWindow(){if(board!=null)board.stop();super.onDetachedFromWindow();}}
