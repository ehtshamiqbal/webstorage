package com.pocketaudio.app;
import android.graphics.*;
import android.graphics.drawable.Drawable;
final class NavIcon extends Drawable {
 private final int kind;private final Paint p=new Paint(3);
 NavIcon(int kind,int size){this.kind=kind;setBounds(0,0,size,size);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(1.7f);p.setColor(0xFF515B6B);}
 public void draw(Canvas c){int save=c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
  if(kind==0){c.drawLine(12,3,12,15,p);c.drawLine(7,10,12,15,p);c.drawLine(12,15,17,10,p);Path tray=new Path();tray.moveTo(4,15);tray.lineTo(4,20);tray.lineTo(20,20);tray.lineTo(20,15);c.drawPath(tray,p);}
  else if(kind==1){c.drawCircle(12,12,8.5f,p);c.drawLine(12,6.5f,12,12,p);c.drawLine(12,12,16,14,p);}
  else{c.drawCircle(12,12,6.7f,p);c.drawCircle(12,12,2.5f,p);for(int i=0;i<8;i++){double a=Math.PI*i/4;c.drawLine(12+(float)Math.cos(a)*7,12+(float)Math.sin(a)*7,12+(float)Math.cos(a)*9.5f,12+(float)Math.sin(a)*9.5f,p);}}
  c.restoreToCount(save);
 }
 public void setTint(int color){p.setColor(color);invalidateSelf();}public void setAlpha(int a){p.setAlpha(a);invalidateSelf();}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
