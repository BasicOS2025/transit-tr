package org.transittr;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Google Material Icons "map", Apache-2.0. Canvas keeps the 24dp icon compatible with API 8. */
final class MapNavDrawable extends Drawable {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path path=new Path();
 MapNavDrawable(int color){
  paint.setColor(color);
  path.moveTo(20.5f,3);path.lineTo(20.34f,3.03f);path.lineTo(15,5.1f);path.lineTo(9,3);path.lineTo(3.36f,4.9f);
  path.cubicTo(3.15f,4.97f,3,5.15f,3,5.38f);path.lineTo(3,20.5f);path.cubicTo(3,20.78f,3.22f,21,3.5f,21);
  path.lineTo(3.66f,20.97f);path.lineTo(9,18.9f);path.lineTo(15,21);path.lineTo(20.64f,19.1f);
  path.cubicTo(20.85f,19.03f,21,18.85f,21,18.62f);path.lineTo(21,3.5f);path.cubicTo(21,3.22f,20.78f,3,20.5f,3);path.close();
  path.moveTo(15,19);path.lineTo(9,16.89f);path.lineTo(9,5);path.lineTo(15,7.11f);path.close();
  path.setFillType(Path.FillType.EVEN_ODD);
 }
 public void draw(Canvas canvas){Rect b=getBounds();int save=canvas.save();canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);canvas.drawPath(path,paint);canvas.restoreToCount(save);}
 public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
 public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
 public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
