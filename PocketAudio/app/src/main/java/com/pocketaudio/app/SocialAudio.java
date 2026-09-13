package com.pocketaudio.app;

import android.content.Context;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.ffmpeg.FFmpeg;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import kotlin.Unit;

final class SocialAudio {
    interface Progress { void update(String title,String message,int percent); boolean cancelled(); }
    static final String PROCESS="pocket-audio";
    private static volatile boolean ready;
    static volatile boolean updating;
    static volatile String updateStatus="Update only if a supported link stops working.";
    private static synchronized void initialize(Context c) throws Exception {
        if(ready)return;
        YoutubeDL.getInstance().init(c.getApplicationContext());
        FFmpeg.getInstance().init(c.getApplicationContext());
        // Install the bundled, checksum-verified engine once for this app version, including upgrades from V5.
        if(c.getSharedPreferences("engine",0).getInt("bundled",0)<6){
            File engine=new File(c.getNoBackupFilesDir(),"youtubedl-android/yt-dlp/yt-dlp");
            File temp=new File(engine.getParentFile(),"engine-new");
            try(InputStream in=c.getAssets().open("yt-dlp")){Files.copy(in,temp.toPath(),StandardCopyOption.REPLACE_EXISTING);}
            Files.move(temp.toPath(),engine.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            c.getSharedPreferences("engine",0).edit().putInt("bundled",6).apply();
        }
        ready=true;
    }
    static void warmUp(Context c){new Thread(()->{try{initialize(c);}catch(Exception|LinkageError ignored){}},"media-warmup").start();}
    static synchronized boolean requestUpdate(Context c){
        if(updating||ConvertService.busy)return false;
        updating=true;updateStatus="Updating video support…";
        new Thread(()->{
            try{initialize(c);YoutubeDL.getInstance().updateYoutubeDL(c,YoutubeDL.UpdateChannel._STABLE);
                updateStatus="Video support is up to date.";
            }catch(Exception|LinkageError e){updateStatus="Update unavailable. Your installed video support is still usable.";}
            finally{updating=false;}
        },"media-support-update").start();return true;
    }
    static void cancel(){NativeMedia.cancel();new Thread(()->{try{YoutubeDL.getInstance().destroyProcessById(PROCESS);}catch(Exception ignored){}},"cancel-extractor").start();}
    static String convert(Context c,String url,File output,int bitrate,boolean video,int height,int fps,Progress p) throws Exception {
        File work=new File(c.getCacheDir(),"social-"+System.nanoTime());
        if(!work.mkdirs())throw new IOException("Could not create temporary storage.");
        try {
            p.update(ready?"Reading video link":"Preparing video tools",ready?"Connecting to the video platform…":"Preparing tools on this phone. First use takes longer.",-1);
            initialize(c);
            if(p.cancelled())throw new CancellationException();
            p.update("Reading video link","Connecting to the video platform…",-1);
            YoutubeDLRequest r=new YoutubeDLRequest(url);
            r.addOption("--cache-dir",new File(c.getNoBackupFilesDir(),"extractor-cache").getAbsolutePath());
            r.addOption("--write-info-json");
            if(PlatformSession.available(c)&&PlatformSession.supports(url))r.addOption("--cookies",PlatformSession.file(c).getAbsolutePath());
            r.addOption("--no-playlist");r.addOption("--playlist-items","1");
            r.addOption("--socket-timeout","20");r.addOption("--retries","2");r.addOption("--extractor-retries","2");
            r.addOption("--max-filesize",String.valueOf(Math.max(1,c.getCacheDir().getUsableSpace()/3)));r.addOption("--no-mtime");r.addOption("--newline");
            if(video){
                String landscape="[height<=?"+height+"]", portrait="[width<=?"+height+"]";
                String base="bv"+landscape+"+ba/bv"+portrait+"+ba/b"+landscape+"/b"+portrait+"/bv"+landscape+"/bv"+portrait;
                String safe="[vcodec^=avc][fps<=?"+fps+"]";
                String preferred="bv"+safe+landscape+"+ba[acodec^=mp4a]/bv"+safe+portrait+"+ba[acodec^=mp4a]/b"+safe+landscape+"[acodec^=mp4a]/b"+safe+portrait+"[acodec^=mp4a]/";
                r.addOption("-f",(height<=1080?preferred:"")+base);
                r.addOption("-S","res:"+height+",fps:"+fps+",ext:mp4:m4a");
                r.addOption("--merge-output-format","mp4");r.addOption("--remux-video","mp4");
            }else{r.addOption("-f","bestaudio/best");r.addOption("-x");r.addOption("--audio-format","mp3");r.addOption("--audio-quality",bitrate+"K");}
            r.addOption("--fragment-retries","3");r.addOption("--concurrent-fragments","4");
            r.addOption("-o",new File(work,"audio.%(ext)s").getAbsolutePath());
            YoutubeDL.getInstance().execute(r,PROCESS,(progress,eta,line)->{
                boolean encoding=line.contains("ExtractAudio")||line.contains("Merger")||line.contains("VideoRemuxer")||line.contains("ffmpeg");
                boolean downloading=line.contains("[download]") && progress>=0;
                if(encoding)p.update(video?"Finishing MP4":"Converting to MP3","Processing your media on this phone.",94);
                else if(downloading)p.update(video?"Downloading video":"Downloading audio","Keep a stable connection. You can switch tabs.",Math.min(90,Math.max(1,(int)(progress*0.9f))));
                else if(line.contains("[info]"))p.update("Selecting your quality","The best available match is being prepared.",-1);
                return Unit.INSTANCE;
            });
            if(p.cancelled())throw new CancellationException();
            File mp3=new File(work,video?"audio.mp4":"audio.mp3");
            if(!mp3.isFile()||mp3.length()==0)throw new IOException("No audio was returned. Check that the link opens a public video.");
            String title=null;
            File metadata=new File(work,"audio.info.json");
            if(metadata.isFile()&&metadata.length()<8*1024*1024){try{title=new org.json.JSONObject(new String(Files.readAllBytes(metadata.toPath()),java.nio.charset.StandardCharsets.UTF_8)).optString("title",null);}catch(Exception ignored){}}
            if(video&&height<=1080)NativeMedia.makeCompatible(c,mp3,height,fps,p);
            Files.move(mp3.toPath(),output.toPath(),StandardCopyOption.REPLACE_EXISTING);
            return title;

        } catch(com.yausername.youtubedl_android.YoutubeDLException e){
            if(p.cancelled())throw new CancellationException();
            String err=String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
            if(err.contains("429")||err.contains("rate-limit")||err.contains("rate limit"))throw new IOException("The platform is temporarily limiting requests. Wait before trying again. This is not a missing app login.");
            if(err.contains("private video")||err.contains("private account")||err.contains("members-only")||err.contains("premium-only"))throw new IOException("This content needs permission from its owner or the correct platform account. The app cannot unlock it.");
            if(err.contains("sign in")||err.contains("login required")||err.contains("login_required")||err.contains("confirm you're not a bot")||err.contains("authentication required"))
                throw new IOException("The platform requires an authenticated session or a verification check. Open the video on the platform first. You can optionally import your own browser session in Settings → Platform connection. Access is still controlled by the platform.");
            if(err.contains("javascript")||err.contains("challenge solving")||err.contains("signature extraction"))throw new IOException("The platform player changed. Use Settings → Update video support, then try again. If it persists, share the video URL and this message.");
            if(err.contains("unsupported url"))throw new IOException("This link is not supported. Paste the full public YouTube video, Short, Instagram Reel/post, or direct MP4 URL.");
            if(err.contains("403"))throw new IOException("The video host refused the download. Try again later with a fresh public link; some videos require login.");
            if(err.contains("unavailable")||err.contains("not available"))throw new IOException("This video is unavailable, removed, or restricted in your region.");
            if(err.contains("no space"))throw new IOException("Not enough phone storage. Free some space and try again.");
            throw new IOException("Could not download media from this link. Check that it is a public video, then retry. The platform may have changed or blocked access.");
        } finally {delete(work);}
    }
    private static void delete(File f){File[] a=f.listFiles();if(a!=null)for(File x:a)delete(x);f.delete();}
}
