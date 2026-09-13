package com.pocketaudio.app;
import android.app.*;
import android.os.Bundle;
import android.media.*;
import android.graphics.*;
import android.net.Uri;
import org.json.*;
public class PlaybackChecks extends Instrumentation {
 private int expected=3;
 public void onCreate(Bundle arguments){super.onCreate(arguments);if(arguments!=null)expected=Integer.parseInt(arguments.getString("expectedVideos","3"));start();}
 public void onStart(){Bundle result=new Bundle();int count=0;
  try{
   android.content.Context context=getTargetContext();
   if(context.getSharedPreferences("engine",0).getInt("bundled",0)!=6)throw new IllegalStateException("New engine not installed");
   com.yausername.youtubedl_android.YoutubeDL.getInstance().init(context);
   com.yausername.youtubedl_android.YoutubeDLRequest request=new com.yausername.youtubedl_android.YoutubeDLRequest("https://example.com");request.addOption("--version");
   String version=com.yausername.youtubedl_android.YoutubeDL.getInstance().execute(request,"engine-test",null).getOut();
   if(!version.contains("2026.08.19"))throw new IllegalStateException("Unexpected engine version: "+version);
   if(PlatformSession.domain("instagram.com.attacker.example")||!PlatformSession.domain(".instagram.com"))throw new IllegalStateException("Session domain boundary failed");
   java.io.File cookie=new java.io.File(context.getCacheDir(),"session-test.txt");
   java.nio.file.Files.write(cookie.toPath(),("# Netscape HTTP Cookie File\n.instagram.com\tTRUE\t/\tTRUE\t0\ttest_only\tfixture\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
   PlatformSession.importFile(context,Uri.fromFile(cookie));if(!PlatformSession.available(context))throw new IllegalStateException("Session import failed");
   java.nio.file.Files.write(cookie.toPath(),"invalid file".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   boolean rejected=false;try{PlatformSession.importFile(context,Uri.fromFile(cookie));}catch(Exception invalid){rejected=true;}
   if(!rejected||!PlatformSession.available(context))throw new IllegalStateException("Invalid import replaced existing session");
   PlatformSession.clear(context);cookie.delete();if(PlatformSession.available(context))throw new IllegalStateException("Session removal failed");
   JSONArray history=new JSONArray(getTargetContext().getSharedPreferences("audio",0).getString("history","[]"));
   for(int j=0;j<history.length();j++){JSONObject item=history.getJSONObject(j);if(!item.getString("mime").startsWith("video"))continue;
    MediaMetadataRetriever r=new MediaMetadataRetriever();try{r.setDataSource(getTargetContext(),Uri.parse(item.getString("uri")));Bitmap frame=r.getFrameAtTime(100000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
     if(frame==null)throw new IllegalStateException("Android could not decode "+item.getString("name"));
     int color=frame.getPixel(frame.getWidth()/2,frame.getHeight()/2);if(Color.blue(color)<30)throw new IllegalStateException("Decoded frame is black for "+item.getString("name"));frame.recycle();count++;
    }finally{r.release();}
   }
   if(count<expected)throw new IllegalStateException("Expected "+expected+" video fixtures, got "+count);
   result.putString("stream","ANDROID_VISIBLE_FRAMES_PASS count="+count);finish(Activity.RESULT_OK,result);
  }catch(Exception e){result.putString("stream","PLAYBACK_FAILURE "+e.toString());finish(Activity.RESULT_CANCELED,result);}
 }
}
