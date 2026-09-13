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
    private Bitmap sample;
    private float[] mesh;
    private int lastX=Integer.MIN_VALUE,lastY,lastW,lastH,lastRootW,lastRootH;
    private float lightX=-1,lightY=-1;
    private boolean pressed;
    GlassMaterial(float radius,float density,boolean strong){this.radius=radius;this.density=density;this.strong=strong;}
    static synchronized Bitmap environment(){
        if(environment!=null)return environment;
        environment=Bitmap.createBitmap(320,640,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(environment);Paint p=new Paint(3);
        p.setShader(new LinearGradient(0,0,320,640,new int[]{0xFFF5F8FC,0xFFD8E2F2,0xFFEAE7F3,0xFFF6F7FB},null,Shader.TileMode.CLAMP));c.drawPaint(p);
        float[][] spots={{12,180,200},{295,310,190},{70,540,210}};
        int[] colors={0xA880A7D2,0x829B8FC5,0x80ADCED7};
        for(int i=0;i<spots.length;i++){float[] s=spots[i];p.setShader(new RadialGradient(s[0],s[1],s[2],new int[]{colors[i],colors[i]&0x00FFFFFF},null,Shader.TileMode.CLAMP));c.drawPaint(p);}
        // Broad, lit folds provide an environment whose contours visibly bend through the lenses.
        Path fold=new Path();fold.moveTo(-100,520);fold.cubicTo(140,80,235,530,435,95);fold.lineTo(430,232);fold.cubicTo(250,635,60,220,-100,665);fold.close();
        p.setShader(new LinearGradient(0,180,280,590,new int[]{0x32FFFFFF,0xBAF6FAFF,0x6CA3B8D6,0x30FFFFFF},null,Shader.TileMode.CLAMP));c.drawPath(fold,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setShader(new LinearGradient(0,180,300,590,0xE5FFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP));c.drawPath(fold,p);
        return environment;
    }
    private void prepare(int x,int y,int width,int height,int rootW,int rootH){
        if(sample!=null&&x==lastX&&y==lastY&&width==lastW&&height==lastH&&rootW==lastRootW&&rootH==lastRootH)return;
        int sw=Math.max(1,Math.min(180,width/4)),sh=Math.max(1,Math.min(240,height/4));
        if(sample==null||sample.getWidth()!=sw||sample.getHeight()!=sh){if(sample!=null)sample.recycle();sample=Bitmap.createBitmap(sw,sh,Bitmap.Config.ARGB_8888);}
        Canvas sc=new Canvas(sample);Paint sp=new Paint(3);sc.scale(sw/(float)width,sh/(float)height);
        sc.drawBitmap(environment(),null,new RectF(-x,-y,rootW-x,rootH-y),sp);
        if(mesh==null)mesh=new float[21*21*2];int index=0;
        for(int j=0;j<=20;j++)for(int i=0;i<=20;i++){
            float nx=i/10f-1,ny=j/10f-1,edge=Math.max(Math.abs(nx),Math.abs(ny));
            float bend=(float)Math.pow(edge,5)*(1-edge)*28*density;
            mesh[index++]=r.left+i*width/20f-nx*bend;mesh[index++]=r.top+j*height/20f-ny*bend;
        }
        lastX=x;lastY=y;lastW=width;lastH=height;lastRootW=rootW;lastRootH=rootH;
    }
    public void draw(Canvas c){
        r.set(getBounds());r.inset(density*.5f,density*.5f);if(r.isEmpty())return;
        Drawable.Callback cb=getCallback();while(cb instanceof Drawable)cb=((Drawable)cb).getCallback();
        View owner=cb instanceof View?(View)cb:null;
        int[] pos={0,0};int width=(int)r.width(),height=(int)r.height();
        if(owner!=null){owner.getLocationInWindow(pos);width=owner.getRootView().getWidth();height=owner.getRootView().getHeight();}
        int save=c.save();clip.reset();clip.addRoundRect(r,radius,radius,Path.Direction.CW);c.clipPath(clip);
        p.setAlpha(alpha);p.setShader(null);p.setStyle(Paint.Style.FILL);
        prepare(pos[0],pos[1],Math.max(1,(int)r.width()),Math.max(1,(int)r.height()),width,height);
        c.drawBitmapMesh(sample,20,20,mesh,0,null,0,p);
        p.setShader(new LinearGradient(0,r.top,r.right,r.bottom,new int[]{strong?0xA3FFFFFF:0x72FFFFFF,strong?0x68FFFFFF:0x28FFFFFF,0x85FFFFFF},new float[]{0,.58f,1},Shader.TileMode.CLAMP));c.drawRoundRect(r,radius,radius,p);
        p.setShader(new LinearGradient(0,r.top,r.right,r.bottom,new int[]{0xEFFFFFFF,0x24FFFFFF,0x19AAB7C8,0xBFFFFFFF},new float[]{0,.35f,.75f,1},Shader.TileMode.CLAMP));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(density*1.1f);c.drawRoundRect(r,radius,radius,p);
        RectF inner=new RectF(r);inner.inset(density*2,density*2);p.setStrokeWidth(density*2.5f);p.setShader(new LinearGradient(0,r.top,0,r.bottom,0x70FFFFFF,0x06FFFFFF,Shader.TileMode.CLAMP));c.drawRoundRect(inner,radius,radius,p);
        if(pressed){p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(lightX<0?r.centerX():lightX,lightY<0?r.centerY():lightY,Math.max(r.width(),r.height()),0x65FFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP));c.drawRoundRect(r,radius,radius,p);}
        p.setShader(null);p.setStyle(Paint.Style.FILL);c.restoreToCount(save);
    }
    public void setHotspot(float x,float y){lightX=x;lightY=y;if(pressed)invalidateSelf();}
    public boolean isStateful(){return true;}
    protected boolean onStateChange(int[] state){boolean down=false;for(int s:state)if(s==android.R.attr.state_pressed)down=true;if(pressed==down)return false;pressed=down;invalidateSelf();return true;}
    public void setAlpha(int value){alpha=value;invalidateSelf();}
    public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}
    public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
