package com.pocketaudio.app;
import android.app.*;
import android.content.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import java.io.*;
import java.net.*;
import java.util.concurrent.CancellationException;
import org.json.*;

public class ConvertService extends Service {
 static volatile boolean busy=false;
 static volatile int progress=0;
 static volatile long startedAt;
 static volatile String status="Ready to download",detail="Paste a public video link.",resultMime="audio/mpeg";
 static volatile Uri result;
 private volatile boolean cancelled;
 private volatile String storageError;
 private PowerManager.WakeLock wake;
 private static final String CHANNEL="conversion";
 private long lastUpdate;
 public IBinder onBind(Intent i){return null;}
 public void onCreate(){super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL,"Media downloads",NotificationManager.IMPORTANCE_LOW));}
 public int onStartCommand(Intent i,int flags,int id){
  if(i!=null&&"cancel".equals(i.getAction())){cancel();return START_NOT_STICKY;}
  if(busy)return START_NOT_STICKY;
  if(SocialAudio.updating){status="Video support is updating";detail="Please wait, then tap Download again.";stopSelf();return START_NOT_STICKY;}
  if(i==null){stopSelf();return START_NOT_STICKY;}
  startedAt=SystemClock.elapsedRealtime();busy=true;cancelled=false;storageError=null;result=null;status="Starting download";detail="Preparing your media…";progress=-1;
  startForeground(7,notification());
  wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"PocketMedia:download");wake.acquire(6*60*60*1000L);
  String url=i.getStringExtra("url");int fps=i.getIntExtra("fps",30);int bitrate=i.getIntExtra("bitrate",192),height=i.getIntExtra("height",1080);boolean video=i.getBooleanExtra("video",false);
  new Thread(()->runJob(url,bitrate,video,height,fps,id),"media-download").start();return START_NOT_STICKY;
 }
 private Notification notification(){
  PendingIntent tap=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,ConvertService.class).setAction("cancel"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_audio).setContentTitle(status).setContentText(detail).setContentIntent(tap).setOnlyAlertOnce(true).setOngoing(true).setProgress(100,Math.max(0,progress),progress<0).addAction(new Notification.Action.Builder(null,"Cancel",stop).build()).build();
 }
 @android.annotation.SuppressLint("MissingPermission")
 private void update(String title,String text,int percent){status=title;detail=text;progress=percent;long now=SystemClock.elapsedRealtime();if(now-lastUpdate>500){getSystemService(NotificationManager.class).notify(7,notification());lastUpdate=now;}}
 private void cancel(){cancelled=true;SocialAudio.cancel();}
 private void check() throws IOException {if(storageError!=null)throw new IOException(storageError);if(cancelled)throw new CancellationException();}
 public void onTimeout(int startId,int type){cancel();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}
 public void onDestroy(){cancel();if(wake!=null&&wake.isHeld())wake.release();super.onDestroy();}
 static String normalize(String text) throws Exception {
  if(text==null)throw new IOException("Paste a link first.");
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("https?://[^\\s<>\\\"]+").matcher(text.trim());
  String s=m.find()?m.group():text.trim();if(!s.contains("://")&&s.matches("(?i)(www\\.|youtu\\.be/|youtube\\.com/|instagram\\.com/|vimeo\\.com/|tiktok\\.com/).*"))s="https://"+s;
  checkedUrl(s);return s;
 }
 static URL checkedUrl(String s) throws Exception {URL u=new URI(s.trim()).toURL();if(!("https".equalsIgnoreCase(u.getProtocol())||"http".equalsIgnoreCase(u.getProtocol()))||u.getHost().isEmpty()||u.getUserInfo()!=null)throw new IOException("Use a public HTTP or HTTPS media link.");return u;}
 private String mediaInfo(File f,boolean video){
  if(!video)return "MP3 audio";
  MediaMetadataRetriever r=new MediaMetadataRetriever();
  try{r.setDataSource(f.getAbsolutePath());String w=r.extractMetadata(18),h=r.extractMetadata(19),fps=r.extractMetadata(25);return w!=null&&h!=null?w+" × "+h+(fps!=null?" · "+fps+" fps":""):"MP4 video";}catch(Exception e){return "MP4 video";}finally{try{r.release();}catch(Exception ignored){}}
 }
 private void runJob(String url,int bitrate,boolean video,int height,int fps,int startId){
  File output=null;Uri pending=null;
  final java.util.concurrent.atomic.AtomicBoolean finished=new java.util.concurrent.atomic.AtomicBoolean(false);
  Thread monitor=new Thread(()->{while(!finished.get()){if(getCacheDir().getUsableSpace()<128L*1024*1024){storageError="Storage is almost full. Free space and retry.";cancel();break;}try{Thread.sleep(1000);}catch(InterruptedException e){break;}}},"storage-monitor");monitor.start();
  try{
   check();output=File.createTempFile("media-",video?".mp4":".mp3",getCacheDir());
   String name=SocialAudio.convert(this,normalize(url),output,bitrate,video,height,fps,new SocialAudio.Progress(){public void update(String t,String d,int p){ConvertService.this.update(t,d,p);}public boolean cancelled(){return cancelled;}});check();
   if(output.length()+128L*1024*1024>getCacheDir().getUsableSpace())throw new IOException("Not enough storage to save the finished file. Free space and retry.");
   String info=mediaInfo(output,video),extension=video?".mp4":".mp3";resultMime=video?"video/mp4":"audio/mpeg";
   String safe=(name==null?(video?"Video":"Audio"):name).replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]"," ").replaceAll("\\s+"," ").trim();
   if(safe.isEmpty())safe=video?"Video":"Audio";
   while(safe.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>180)safe=safe.substring(0,safe.offsetByCodePoints(safe.length(),-1));
   String filename=safe+extension;
   String folder=(video?Environment.DIRECTORY_MOVIES:Environment.DIRECTORY_MUSIC)+"/PocketMedia";
   ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,filename);v.put(MediaStore.MediaColumns.MIME_TYPE,resultMime);v.put(MediaStore.MediaColumns.RELATIVE_PATH,folder);v.put(MediaStore.MediaColumns.IS_PENDING,1);
   Uri collection=video?MediaStore.Video.Media.EXTERNAL_CONTENT_URI:MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
   pending=getContentResolver().insert(collection,v);if(pending==null)throw new IOException("Cannot create a file in your media library.");
   update("Saving to phone",info+" · "+folder,96);
   try(InputStream in=new FileInputStream(output);OutputStream out=getContentResolver().openOutputStream(pending)){if(out==null)throw new IOException("Cannot save media.");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){check();out.write(b,0,n);}}
   check();v.clear();v.put(MediaStore.MediaColumns.IS_PENDING,0);if(getContentResolver().update(pending,v,null,null)<1)throw new IOException("Could not publish the saved file.");
   result=pending;pending=null;
   android.content.SharedPreferences prefs=getSharedPreferences("audio",0);
   JSONArray history;try{history=new JSONArray(prefs.getString("history","[]"));}catch(Exception e){history=new JSONArray();}
   JSONObject item=new JSONObject().put("uri",result.toString()).put("name",filename).put("mime",resultMime).put("info",info);
   JSONArray next=new JSONArray();next.put(item);for(int i=0;i<Math.min(19,history.length());i++)next.put(history.get(i));
   prefs.edit().putString("last",result.toString()).putString("mime",resultMime).putString("filename",filename).putString("history",next.toString()).apply();
   update("Download complete",info+"\n"+filename+"\nSaved in "+folder,100);
  }catch(Exception|LinkageError e){
   if(storageError!=null)update("Download stopped",storageError,0);
   else if(cancelled||e instanceof CancellationException)update("Download cancelled","You can retry with the same link.",0);
   else update("Couldn’t download this link",e instanceof IOException&&e.getMessage()!=null?e.getMessage():"Could not process this media. Retry with a public supported link.",0);
  }finally{
   finished.set(true);monitor.interrupt();if(pending!=null)try{getContentResolver().delete(pending,null,null);}catch(Exception ignored){}if(output!=null)output.delete();
   stopForeground(STOP_FOREGROUND_REMOVE);if(wake!=null&&wake.isHeld())wake.release();busy=false;stopSelf(startId);
  }
 }
}
