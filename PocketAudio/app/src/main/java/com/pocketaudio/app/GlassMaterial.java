package com.pocketaudio.app;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Samples the same softly diffused environment beneath each optical surface. */
final class GlassMaterial extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF r=new RectF();
    private final Path clip=new Path();
    private final float radius,density;
    private final boolean strong;
    private int alpha=255;
    private static Bitmap environment;
    GlassMaterial(float radius,float density,boolean strong){this.radius=radius;this.density=density;this.strong=strong;}
    static synchronized Bitmap environment(){
        if(environment!=null)return environment;
        environment=Bitmap.createBitmap(180,360,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(environment);Paint p=new Paint(3);
        p.setShader(new LinearGradient(0,0,180,360,new int[]{0xFFF7F7FA,0xFFE3E7EF,0xFFF7F4F2},null,Shader.TileMode.CLAMP));c.drawPaint(p);
        float[][] spots={{18,100,100},{174,183,116},{30,300,110}};
        int[] colors={0xB5AABDDD,0x90D2BCD9,0x85B8D9DE};
        for(int i=0;i<spots.length;i++){float[] s=spots[i];p.setShader(new RadialGradient(s[0],s[1],s[2],new int[]{colors[i],colors[i]&0x00FFFFFF},null,Shader.TileMode.CLAMP));c.drawPaint(p);}
        return environment;
    }
    public void draw(Canvas c){
        r.set(getBounds());r.inset(density*.5f,density*.5f);if(r.isEmpty())return;
        Drawable.Callback cb=getCallback();while(cb instanceof Drawable)cb=((Drawable)cb).getCallback();
        View owner=cb instanceof View?(View)cb:null;
        int[] pos={0,0};int width=(int)r.width(),height=(int)r.height();
        if(owner!=null){owner.getLocationInWindow(pos);width=owner.getRootView().getWidth();height=owner.getRootView().getHeight();}
        int save=c.save();clip.reset();clip.addRoundRect(r,radius,radius,Path.Direction.CW);c.clipPath(clip);
        p.setAlpha(alpha);p.setShader(null);p.setStyle(Paint.Style.FILL);
        // Slight magnification supplies background-relative edge refraction without blurring text.
        c.drawBitmap(environment(),null,new RectF(-pos[0]-width*.008f,-pos[1]-height*.008f,width*1.008f-pos[0],height*1.008f-pos[1]),p);
        p.setShader(new LinearGradient(0,r.top,r.right,r.bottom,new int[]{strong?0xC9FFFFFF:0x8CFFFFFF,strong?0x8FFFFFFF:0x50FFFFFF,0xA8FFFFFF},new float[]{0,.64f,1},Shader.TileMode.CLAMP));c.drawRoundRect(r,radius,radius,p);
        p.setShader(new LinearGradient(0,r.top,r.right,r.bottom,new int[]{0xEFFFFFFF,0x24FFFFFF,0x19AAB7C8,0xBFFFFFFF},new float[]{0,.35f,.75f,1},Shader.TileMode.CLAMP));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(density*1.1f);c.drawRoundRect(r,radius,radius,p);
        RectF inner=new RectF(r);inner.inset(density*2,density*2);p.setStrokeWidth(density*2);p.setShader(new LinearGradient(0,r.top,0,r.bottom,0x4AFFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP));c.drawRoundRect(inner,radius,radius,p);
        p.setShader(null);p.setStyle(Paint.Style.FILL);c.restoreToCount(save);
    }
    public void setAlpha(int value){alpha=value;invalidateSelf();}
    public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
    public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
