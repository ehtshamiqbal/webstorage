package com.pocketaudio.app;

import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;

/** Optional user-owned Netscape session file; kept only in private, non-backed-up storage. */
final class PlatformSession {
    static File file(Context c){return new File(c.getNoBackupFilesDir(),"platform-session.txt");}
    static boolean available(Context c){return file(c).isFile();}
    static boolean domain(String domain){
        String d=domain.toLowerCase(Locale.ROOT);if(d.startsWith("."))d=d.substring(1);
        for(String root:new String[]{"youtube.com","google.com","instagram.com","facebook.com"})if(d.equals(root)||d.endsWith("."+root))return true;
        return false;
    }
    static boolean supports(String url){try{return domain(new java.net.URI(url).getHost());}catch(Exception e){return false;}}
    static void importFile(Context c,Uri uri)throws Exception{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("File unavailable.");byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(bytes.size()+n>1048576)throw new IOException("Session file is too large (maximum 1 MB).");bytes.write(b,0,n);}}
        StringBuilder clean=new StringBuilder("# Netscape HTTP Cookie File\n");int count=0;
        for(String line:new String(bytes.toByteArray(),StandardCharsets.UTF_8).split("\\r?\\n")){
            if(line.startsWith("#HttpOnly_"))line=line.substring(10);else if(line.startsWith("#")||line.trim().isEmpty())continue;
            String[] cols=line.split("\\t",-1);if(cols.length!=7)throw new IOException("Choose a Netscape-format cookies.txt file exported from your own signed-in browser.");
            if(!domain(cols[0]))continue;
            long expiry;try{expiry=Long.parseLong(cols[4]);}catch(Exception e){throw new IOException("Invalid cookie expiry.");}
            if(expiry!=0&&expiry<System.currentTimeMillis()/1000)continue;
            if(!cols[1].matches("TRUE|FALSE")||!cols[3].matches("TRUE|FALSE")||!cols[2].startsWith("/")||cols[5].isEmpty())throw new IOException("Invalid session file.");
            clean.append(line).append('\n');count++;
        }
        if(count==0)throw new IOException("No current YouTube or Instagram session found in this file.");
        File tmp=new File(c.getNoBackupFilesDir(),"session-import.tmp");
        try{Files.write(tmp.toPath(),clean.toString().getBytes(StandardCharsets.UTF_8));Files.move(tmp.toPath(),file(c).toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{tmp.delete();}
    }
    static void clear(Context c)throws IOException{Files.deleteIfExists(file(c).toPath());}
}
