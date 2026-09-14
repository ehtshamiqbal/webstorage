package com.pocketaudio.app;

import android.content.Context;
import java.io.*;
import java.util.*;
import java.util.concurrent.CancellationException;
import org.json.*;

final class NativeMedia {
    private static volatile Process active;
    static void cancel(){Process p=active;if(p!=null)p.destroy();}
    private static String run(Context c,String binary,List<String> args,SocialAudio.Progress progress) throws Exception {
        if(progress.cancelled())throw new CancellationException();
        List<String> command=new ArrayList<>();command.add(new File(c.getApplicationInfo().nativeLibraryDir,binary).getAbsolutePath());command.addAll(args);
        ProcessBuilder builder=new ProcessBuilder(command).redirectErrorStream(true);
        File packages=new File(c.getNoBackupFilesDir(),"youtubedl-android/packages");
        builder.environment().put("LD_LIBRARY_PATH",new File(packages,"ffmpeg/usr/lib").getAbsolutePath()+":"+new File(packages,"python/usr/lib").getAbsolutePath());
        builder.environment().put("TMPDIR",c.getCacheDir().getAbsolutePath());
        Process process=builder.start();active=process;
        try {if(progress.cancelled()){process.destroy();throw new CancellationException();}
            StringBuilder text=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()))){String line;while((line=reader.readLine())!=null){if(progress.cancelled()){process.destroy();throw new CancellationException();}text.append(line).append('\n');if(text.length()>262144)text.delete(0,text.length()-131072);}}
            int exit=process.waitFor();if(progress.cancelled())throw new CancellationException();if(exit!=0)throw new IOException("Could not prepare a playable video. Try 720p Gallery safe, or another public source.");return text.toString();
        }finally{active=null;if(process.isAlive())process.destroy();}
    }
    static void makeCompatible(Context c,File file,int limit,int fps,SocialAudio.Progress p) throws Exception {
        p.update("Checking video compatibility","Checking the video and audio codecs for Gallery.",92);
        JSONObject info=new JSONObject(run(c,"libffprobe.so",Arrays.asList("-v","error","-show_entries","stream=codec_type,codec_name,pix_fmt,width,height,r_frame_rate","-of","json",file.getAbsolutePath()),p));
        JSONArray streams=info.getJSONArray("streams");JSONObject video=null,audio=null;
        for(int j=0;j<streams.length();j++){JSONObject s=streams.getJSONObject(j);if("video".equals(s.optString("codec_type"))&&video==null)video=s;if("audio".equals(s.optString("codec_type"))&&audio==null)audio=s;}
        if(video==null)throw new IOException("This source did not contain a video track.");
        int w=video.optInt("width"),h=video.optInt("height");if(w<=0||h<=0)throw new IOException("The source video dimensions could not be read.");
        double rate=0;try{String[] parts=video.optString("r_frame_rate","0/1").split("/");rate=Double.parseDouble(parts[0])/Double.parseDouble(parts.length>1?parts[1]:"1");}catch(Exception ignored){}
        int longLimit=(int)Math.round(limit*16.0/9);double scale=Math.min(1,Math.min((double)limit/Math.min(w,h),(double)longLimit/Math.max(w,h)));
        int targetW=Math.max(2,((int)(w*scale)/2)*2),targetH=Math.max(2,((int)(h*scale)/2)*2);
        boolean encodeVideo=!"h264".equals(video.optString("codec_name"))||!"yuv420p".equals(video.optString("pix_fmt"))||targetW!=w||targetH!=h||rate>fps+0.1;
        boolean encodeAudio=audio!=null&&!"aac".equals(audio.optString("codec_name"));
        if(!encodeVideo&&!encodeAudio)return;
        p.update("Preparing Gallery-safe video",encodeVideo?"Converting to H.264. This takes longer, but improves phone playback.":"Keeping the video; converting its audio to AAC.",94);
        File compatible=new File(file.getParentFile(),"compatible.mp4");
        List<String> args=new ArrayList<>(Arrays.asList("-y","-nostdin","-v","error","-i",file.getAbsolutePath(),"-map","0:v:0","-map","0:a:0?","-map_metadata","0","-c:v",encodeVideo?"libx264":"copy"));
        if(encodeVideo){args.addAll(Arrays.asList("-preset","veryfast","-crf","20","-pix_fmt","yuv420p","-threads","2","-vf","scale="+targetW+":"+targetH+(rate>fps+0.1?",fps="+fps:"")));}
        args.addAll(Arrays.asList("-c:a",encodeAudio?"aac":"copy"));if(encodeAudio)args.addAll(Arrays.asList("-b:a","192k","-ac","2"));
        args.addAll(Arrays.asList("-movflags","+faststart",compatible.getAbsolutePath()));
        try{run(c,"libffmpeg.so",args,p);if(!compatible.isFile()||compatible.length()==0)throw new IOException("No playable output was produced.");java.nio.file.Files.move(compatible.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}finally{compatible.delete();}
    }
}
