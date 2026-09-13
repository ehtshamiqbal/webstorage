package com.pocketaudio.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;

public class MainActivity extends Activity {
    private final int ink=0xFF1C2430,muted=0xFF515B6B,accent=0xFF176BDF;
    private EditText link;
    private android.animation.AnimatorSet splashAnimation;
    private Spinner mode,quality;
    private Button convert,cancel,update;
    private TextView heading,detail,updateDetail;
    private ProgressBar progress;
    private LinearLayout historyList;
    private FrameLayout pages;
    private View[] screens;
    private Button[] tabs;
    private int selectedTab,lastMode=-1,pendingQuality=-1;
    private boolean pendingStart;
    private long pressedAt;
    private String historyVersion="",renderVersion="";
    private boolean importingSession;
    private TextView sessionStatus;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){render();handler.postDelayed(this,500);}};
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private Drawable glass(int radius,boolean bright){
        return new GlassMaterial(dp(radius),getResources().getDisplayMetrics().density,bright);
    }
    private Drawable ripple(Drawable d,int radius){return new RippleDrawable(ColorStateList.valueOf(0x224258DA),d,shape(Color.WHITE,radius));}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    private void gap(LinearLayout l,int h){l.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(h)));}
    private Button button(String value,boolean primary){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?Color.WHITE:accent);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(12),0,dp(12),0);
        Drawable d=glass(18,true);if(primary){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF4195F1,0xFF1263D7});g.setCornerRadius(dp(18));d=g;}
        b.setBackground(ripple(d,18));return b;}
    private EditText input(String hint,int id){EditText e=new EditText(this);e.setId(id);e.setHint(hint);e.setTextColor(ink);e.setHintTextColor(muted);e.setTextSize(15);e.setSingleLine(true);e.setPadding(dp(13),0,dp(13),0);e.setBackground(ripple(glass(14,false),14));return e;}
    private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(16),dp(16),dp(16),dp(16));l.setBackground(glass(24,true));l.setElevation(dp(5));return l;}
    private ScrollView scrolling(View child){ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setPadding(dp(20),dp(6),dp(20),dp(12));s.setVerticalScrollBarEnabled(false);s.addView(child);return s;}
    public void onCreate(Bundle state){
        setTheme(R.style.AppTheme);super.onCreate(state);
        if(Build.VERSION.SDK_INT>=31)getSplashScreen().setOnExitAnimationListener(android.window.SplashScreenView::remove);
        SocialAudio.warmUp(getApplicationContext());
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(0xFFF0F1F5);
        FrameLayout scene=new FrameLayout(this);scene.addView(new LiquidBackground(this),new FrameLayout.LayoutParams(-1,-1));
        LinearLayout root=column();scene.addView(root,new FrameLayout.LayoutParams(-1,-1));
        if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,i)->{Insets bars=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return i;});
        else root.setFitsSystemWindows(true);
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);brand.setPadding(dp(22),dp(7),dp(22),dp(7));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_media);brand.addView(logo,new LinearLayout.LayoutParams(dp(34),dp(38)));
        TextView title=text("  Pocket Media",20,ink,true);brand.addView(title,new LinearLayout.LayoutParams(0,dp(38),1));title.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge=text("ON DEVICE",10,accent,true);badge.setPadding(dp(10),dp(7),dp(10),dp(7));badge.setBackground(glass(20,false));brand.addView(badge);root.addView(brand,new LinearLayout.LayoutParams(-1,dp(60)));
        pages=new FrameLayout(this);root.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
        screens=new View[]{buildHome(),buildHistory(),buildSettings()};for(View s:screens)pages.addView(s,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(5),dp(5),dp(5),dp(5));nav.setBackground(glass(28,true));nav.setElevation(dp(5));
        tabs=new Button[3];String[] names={"Download","Recent","Settings"};
        for(int j=0;j<3;j++){final int index=j;tabs[j]=button(names[j],false);tabs[j].setTextSize(11);tabs[j].setCompoundDrawables(null,new NavIcon(j,dp(21)),null,null);tabs[j].setCompoundDrawablePadding(dp(3));nav.addView(tabs[j],new LinearLayout.LayoutParams(0,dp(56),1));tabs[j].setOnClickListener(v->selectTab(index,true));}
        LinearLayout.LayoutParams navLp=new LinearLayout.LayoutParams(-1,dp(66));navLp.setMargins(dp(16),dp(4),dp(16),dp(10));root.addView(nav,navLp);
        setContentView(scene);
        SharedPreferences prefs=getSharedPreferences("draft",0);int savedMode=state!=null?state.getInt("mode"):prefs.getInt("mode",0);
        pendingQuality=state!=null?state.getInt("quality",savedMode==1?2:1):prefs.getInt("quality",savedMode==1?2:1);
        mode.setSelection(savedMode);setQualities(savedMode);
        link.setText(state!=null?state.getString("link",""):prefs.getString("link",""));
        selectTab(state!=null?state.getInt("tab",0):0,false);
        if(state==null)receiveSharedLink(getIntent());
        render();
        if(state==null&&!ConvertService.busy&&!Intent.ACTION_SEND.equals(getIntent().getAction()))showSplash(scene);
    }
    private View buildHome(){
        LinearLayout home=column();TextView title=text("Your media. Beautifully saved.",23,ink,true);home.addView(title);gap(home,5);home.addView(text("Video or audio. Straight to your phone.",13,muted,false));gap(home,16);
        LinearLayout c=card();home.addView(c);
        c.addView(text("VIDEO LINK",10,accent,true));gap(c,7);
        LinearLayout row=new LinearLayout(this);link=input("Paste your video link",R.id.link_input);link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);row.addView(link,new LinearLayout.LayoutParams(0,dp(48),1));
        Button paste=button("Paste",false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(72),dp(48));p.setMarginStart(dp(6));row.addView(paste,p);c.addView(row);
        paste.setOnClickListener(v->{if(ConvertService.busy||pendingStart)return;ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm.hasPrimaryClip()&&cm.getPrimaryClip()!=null){link.setText(cm.getPrimaryClip().getItemAt(0).coerceToText(this));link.setSelection(link.length());}else Toast.makeText(this,"Copy a video link first",Toast.LENGTH_SHORT).show();});
        gap(c,13);LinearLayout labels=new LinearLayout(this);labels.addView(text("FORMAT",10,accent,true),new LinearLayout.LayoutParams(0,-2,1));labels.addView(text("QUALITY",10,accent,true),new LinearLayout.LayoutParams(0,-2,1));c.addView(labels);gap(c,5);
        LinearLayout choices=new LinearLayout(this);mode=new Spinner(this);mode.setId(R.id.mode_input);mode.setBackground(ripple(glass(14,false),14));mode.setAdapter(adapter(new String[]{"Audio · MP3","Video · MP4"}));quality=new Spinner(this);quality.setId(R.id.quality_input);quality.setBackground(ripple(glass(14,false),14));
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,dp(48),1);mp.setMarginEnd(dp(7));choices.addView(mode,mp);choices.addView(quality,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(choices);
        mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int pos,long id){setQualities(pos);}});
        gap(c,15);TextView auto=text("Original title. Automatically named.",12,muted,false);c.addView(auto);
        gap(c,7);c.addView(text("720p / 1080p use Gallery-safe H.264. Higher resolutions keep original codecs and need a compatible player.",11,muted,false));gap(c,15);
        convert=button("Download MP3",true);convert.setId(R.id.download_button);c.addView(convert,new LinearLayout.LayoutParams(-1,dp(52)));convert.setOnClickListener(v->start());
        gap(home,15);heading=text("Ready when you are",15,ink,true);home.addView(heading);gap(home,4);detail=text("MP3 to Music · MP4 to Gallery",12,muted,false);detail.setMaxLines(2);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);home.addView(detail);
        heading.setOnClickListener(v->{if(ConvertService.result!=null&&!ConvertService.busy)selectTab(1,true);});
        detail.setOnClickListener(v->{if(!ConvertService.detail.isEmpty())new AlertDialog.Builder(this).setTitle(ConvertService.status).setMessage(ConvertService.detail).setPositiveButton("OK",null).show();});
        gap(home,8);progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setProgressTintList(ColorStateList.valueOf(accent));home.addView(progress,new LinearLayout.LayoutParams(-1,dp(5)));
        cancel=button("Cancel download",false);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(48));cp.topMargin=dp(7);home.addView(cancel,cp);cancel.setOnClickListener(v->{startService(new Intent(this,ConvertService.class).setAction("cancel"));cancel.setEnabled(false);cancel.setText("Cancelling…");});
        return scrolling(home);
    }
    private ArrayAdapter<String> adapter(String[] values){return new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,values){
        public View getView(int pos,View view,android.view.ViewGroup parent){TextView t=(TextView)super.getView(pos,view,parent);t.setTextSize(13);t.setTextColor(ink);t.setSingleLine();t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setPadding(dp(10),0,dp(5),0);return t;}
        public View getDropDownView(int pos,View view,android.view.ViewGroup parent){TextView t=(TextView)super.getDropDownView(pos,view,parent);t.setTextColor(ink);t.setTextSize(15);t.setPadding(dp(16),dp(14),dp(16),dp(14));return t;}
    };}
    private void setQualities(int selected){if(lastMode==selected)return;lastMode=selected;
        String[] q=selected==1?new String[]{"Best · Original","720p · Gallery safe","1080p · Gallery safe","1080p60 · Gallery safe","4K · Original","6K · Original","8K · Original"}:new String[]{"128 kbps","192 kbps","320 kbps"};
        quality.setAdapter(adapter(q));quality.setSelection(pendingQuality>=0?Math.min(pendingQuality,q.length-1):(selected==1?2:1));pendingQuality=-1;
    }
    private View buildHistory(){LinearLayout l=column();l.addView(text("Your collection",26,ink,true));gap(l,5);l.addView(text("Recent downloads · Always on your phone",13,muted,false));gap(l,18);historyList=column();l.addView(historyList);return scrolling(l);}
    private void refreshHistory(){String value=getSharedPreferences("audio",0).getString("history","[]");if(value.equals(historyVersion))return;historyVersion=value;historyList.removeAllViews();
        try{JSONArray items=new JSONArray(value);if(items.length()==0){LinearLayout empty=card();empty.setPadding(dp(22),dp(36),dp(22),dp(36));empty.addView(text("Your first save starts here",20,ink,true));gap(empty,10);empty.addView(text("Download a video or audio track. It will appear here, ready to open or share.",14,muted,false));gap(empty,20);Button back=button("Download something",true);empty.addView(back);back.setOnClickListener(v->selectTab(0,true));historyList.addView(empty);}
            for(int j=0;j<items.length();j++){JSONObject item=items.getJSONObject(j);Uri uri=Uri.parse(item.getString("uri"));String mime=item.getString("mime");LinearLayout c=card();historyList.addView(c);c.addView(text(mime.startsWith("video")?"MP4  ·  VIDEO":"MP3  ·  AUDIO",10,accent,true));gap(c,7);TextView filename=text(item.getString("name"),15,ink,true);filename.setMaxLines(2);filename.setEllipsize(android.text.TextUtils.TruncateAt.END);c.addView(filename);gap(c,5);c.addView(text(item.optString("info","Saved on your phone"),12,muted,false));gap(c,12);LinearLayout actions=new LinearLayout(this);Button open=button("Open",true),share=button("Share",false);LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(48),1);a.setMarginEnd(dp(8));actions.addView(open,a);actions.addView(share,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(actions);open.setOnClickListener(v->useMedia(uri,mime,false));share.setOnClickListener(v->useMedia(uri,mime,true));gap(historyList,12);}
        }catch(JSONException e){historyList.addView(text("Could not read recent downloads. Your files are still in Music and Movies / PocketMedia.",14,muted,false));}}
    private View buildSettings(){LinearLayout l=column();l.addView(text("A few essentials",26,ink,true));gap(l,5);l.addView(text("Your preferences, privacy and connections.",13,muted,false));gap(l,18);
        LinearLayout c=card();l.addView(c);c.addView(text("Video support",18,ink,true));gap(c,8);c.addView(text("Downloads use the installed tools immediately. Check for an update here if a public link stops working.",13,muted,false));gap(c,14);update=button("Update video support",true);c.addView(update,new LinearLayout.LayoutParams(-1,dp(48)));updateDetail=text(SocialAudio.updateStatus,12,muted,false);gap(c,10);c.addView(updateDetail);update.setOnClickListener(v->{if(!SocialAudio.requestUpdate(getApplicationContext()))Toast.makeText(this,"Finish the current download or update first.",Toast.LENGTH_SHORT).show();render();});
        gap(l,14);LinearLayout session=card();l.addView(session);session.addView(text("Platform connection",18,ink,true));gap(session,8);
        session.addView(text("Public videos need no app account. If a platform requires sign-in, you can optionally import a Netscape cookies.txt session from your own browser. This does not unlock content your account cannot access. Treat the file like a password; never share it.",13,muted,false));gap(session,12);
        Button connect=button("Import my browser session",false);session.addView(connect);connect.setOnClickListener(v->{if(ConvertService.busy||importingSession){Toast.makeText(this,"Finish the current task first.",Toast.LENGTH_SHORT).show();return;}Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);try{startActivityForResult(pick,61);}catch(Exception e){Toast.makeText(this,"No file picker available.",Toast.LENGTH_SHORT).show();}});
        Button disconnect=button("Remove saved session",false);session.addView(disconnect);disconnect.setOnClickListener(v->{if(ConvertService.busy||importingSession)return;try{PlatformSession.clear(this);sessionStatus.setText("No browser session saved.");}catch(Exception e){Toast.makeText(this,"Could not remove session. Retry.",Toast.LENGTH_SHORT).show();}});
        sessionStatus=text(PlatformSession.available(this)?"Session saved on this phone only.":"No browser session saved.",12,muted,false);gap(session,8);session.addView(sessionStatus);
        gap(l,14);LinearLayout storage=card();l.addView(storage);storage.addView(text("Saved where you need it",18,ink,true));gap(storage,8);storage.addView(text("Videos → Gallery / Movies / PocketMedia\nAudio → Music / PocketMedia\n\nGallery-safe downloads use H.264 video and AAC audio, up to 1080p. A compatible source downloads faster; other codecs need conversion.\n\n4K, 6K, 8K and Best retain original codecs. Some phone galleries cannot play these files. Use a compatible player or download again at 1080p Gallery safe.\n\nPrivate, login-required and protected videos may not download. Platforms may also limit requests.",13,muted,false));
        gap(l,16);Button about=button("About & open-source licenses",false);l.addView(about);about.setOnClickListener(v->about());gap(l,10);l.addView(text("POCKET MEDIA 7.0  ·  LIQUID GLASS",11,muted,true));return scrolling(l);}
    private void selectTab(int index,boolean animate){selectedTab=Math.max(0,Math.min(2,index));for(int j=0;j<3;j++){screens[j].setVisibility(j==selectedTab?View.VISIBLE:View.GONE);tabs[j].setTextColor(j==selectedTab?accent:muted);tabs[j].setBackground(ripple(j==selectedTab?glass(23,true):shape(Color.TRANSPARENT,23),23));tabs[j].setSelected(j==selectedTab);tabs[j].getCompoundDrawables()[1].setTint(j==selectedTab?accent:muted);}if(selectedTab==1)refreshHistory();
        if(animate){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(link.getWindowToken(),0);if(android.animation.ValueAnimator.areAnimatorsEnabled()){View active=screens[selectedTab];active.animate().cancel();active.setAlpha(0.6f);active.animate().alpha(1).setDuration(140).start();}}}
    private void start(){if(ConvertService.busy||pendingStart||SocialAudio.updating||importingSession)return;
        try{link.setText(ConvertService.normalize(link.getText().toString()));}catch(Exception e){link.setError("Paste a full public video URL");link.requestFocus();return;}
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(link.getWindowToken(),0);link.clearFocus();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},5);
        boolean video=mode.getSelectedItemPosition()==1;int q=quality.getSelectedItemPosition();
        Intent i=new Intent(this,ConvertService.class).putExtra("url",link.getText().toString().trim()).putExtra("video",video).putExtra("fps",video&&(q==3||q==0||q>=4)?60:30).putExtra("height",video?new int[]{4320,720,1080,1080,2160,3240,4320}[q]:1080).putExtra("bitrate",video?192:new int[]{128,192,320}[q]);
        pendingStart=true;pressedAt=SystemClock.elapsedRealtime();render();
        try{startForegroundService(i);}catch(Exception e){pendingStart=false;render();Toast.makeText(this,"Could not start. Keep the app open and retry.",Toast.LENGTH_LONG).show();}}
    private void render(){if(convert==null)return;boolean b=ConvertService.busy;if(b||SystemClock.elapsedRealtime()-pressedAt>4000)pendingStart=false;boolean working=b||pendingStart;
        String snapshot=b+"|"+pendingStart+"|"+SocialAudio.updating+"|"+SocialAudio.updateStatus+"|"+ConvertService.status+"|"+ConvertService.detail+"|"+ConvertService.progress+"|"+ConvertService.result+"|"+mode.getSelectedItemPosition()+"|"+selectedTab+"|"+(b?(SystemClock.elapsedRealtime()-ConvertService.startedAt)/1000:0);
        if(snapshot.equals(renderVersion))return;renderVersion=snapshot;
        convert.setEnabled(!working&&!SocialAudio.updating);link.setEnabled(!working);quality.setEnabled(!working);mode.setEnabled(!working);convert.setText(working?"Working…":SocialAudio.updating?"Updating tools…":mode.getSelectedItemPosition()==1?"Download MP4":"Download MP3");
        heading.setText(pendingStart?"Starting now…":ConvertService.result!=null&&!b?"Saved · View in Recent":ConvertService.status);
        String d=ConvertService.detail;if(b){long seconds=(SystemClock.elapsedRealtime()-ConvertService.startedAt)/1000;d=seconds+"s · "+d;if(seconds>=15&&ConvertService.progress<0)d=seconds+"s · Waiting for the platform. You can switch tabs or cancel.";}
        detail.setText(pendingStart?"Opening your link…":d);progress.setVisibility(working?View.VISIBLE:View.GONE);progress.setIndeterminate(pendingStart||ConvertService.progress<0);if(ConvertService.progress>=0)progress.setProgress(ConvertService.progress);
        if(!b){cancel.setText("Cancel download");cancel.setEnabled(true);}cancel.setVisibility(b?View.VISIBLE:View.GONE);
        update.setEnabled(!working&&!SocialAudio.updating);update.setText(SocialAudio.updating?"Updating…":"Update video support");updateDetail.setText(SocialAudio.updateStatus);if(selectedTab==1)refreshHistory();}
    private void useMedia(Uri uri,String mime,boolean sharing){try{try(android.content.res.AssetFileDescriptor f=getContentResolver().openAssetFileDescriptor(uri,"r")){if(f==null)throw new java.io.FileNotFoundException();}
        Intent i=new Intent(sharing?Intent.ACTION_SEND:Intent.ACTION_VIEW);if(sharing){i.setType(mime);i.putExtra(Intent.EXTRA_STREAM,uri);i.setClipData(ClipData.newRawUri("Saved media",uri));}else i.setDataAndType(uri,mime);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,sharing?"Share media":"Open media"));
        }catch(Exception e){Toast.makeText(this,"File unavailable, or no compatible player. Check Music / Movies → PocketMedia in Files.",Toast.LENGTH_LONG).show();}}
    private void about(){
        LinearLayout content=column();content.setPadding(dp(22),dp(20),dp(22),dp(22));
        content.addView(text("Pocket Media",26,ink,true));gap(content,4);content.addView(text("Version 7.0  ·  Made for your moments",13,muted,false));gap(content,22);
        aboutSection(content,"How it works","Paste a public video link and choose audio or video. The original platform title becomes the filename automatically. Processing happens on your phone.");
        aboutSection(content,"Playback & quality","Choose 720p or 1080p Gallery safe for H.264 video with AAC audio. Other codecs are converted only when needed. Original-quality options retain the source codecs; playback support varies by phone. No artificial upscaling.");
        aboutSection(content,"Storage & privacy","Videos are saved in Movies / PocketMedia; audio in Music / PocketMedia. Temporary files are removed after completion or handled cancellation. No app account, ads or analytics. Optional browser sessions stay in private phone storage and are excluded from backup. The video platform receives requests from your connection.");
        aboutSection(content,"Source & licensing","Pocket Media is open-source under GNU GPL version 3. Third-party components retain their own licenses. Use content you own or have permission to download.");
        linkButton(content,"View application source","https://github.com/ehtshamiqbal/webstorage/tree/pocket-audio-app/PocketAudio");
        Button gpl=button("Read GPL-3.0 license",false);content.addView(gpl);gpl.setOnClickListener(v->license("GNU GPL version 3","GPL-3.0.txt"));gap(content,20);
        content.addView(text("Open-source components",20,ink,true));gap(content,12);
        component(content,"yt-dlp","Finds public media streams and original titles. Unlicense; bundled dependencies may have additional terms.","https://github.com/yt-dlp/yt-dlp");
        component(content,"youtubedl-android · 0.18.1","Android integration for yt-dlp and its native tools. GPL-3.0.","https://github.com/yausername/youtubedl-android");
        component(content,"FFmpeg","Media inspection, merging and codec conversion. LGPL / GPL terms depend on the bundled build and enabled libraries.","https://ffmpeg.org/legal.html");
        component(content,"LAME · 3.100","MP3 encoding component. LGPL version 2.0 or later.","https://lame.sourceforge.io/");
        Button lame=button("Read LAME license",false);content.addView(lame);lame.setOnClickListener(v->license("LAME license","LAME-LICENSE.txt"));
        ScrollView scroll=new ScrollView(this);scroll.addView(content);new AlertDialog.Builder(this).setView(scroll).setPositiveButton("Done",null).show();
    }
    private void aboutSection(LinearLayout l,String title,String body){l.addView(text(title,18,ink,true));gap(l,7);TextView t=text(body,14,muted,false);t.setLineSpacing(dp(3),1);l.addView(t);gap(l,20);}
    private void component(LinearLayout l,String title,String body,String url){aboutSection(l,title,body);linkButton(l,"Project & license ↗",url);gap(l,14);}
    private void linkButton(LinearLayout l,String title,String url){Button b=button(title,false);l.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));b.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){Toast.makeText(this,"No browser available",Toast.LENGTH_SHORT).show();}});gap(l,8);}
    private void license(String title,String asset){String value;try(java.io.InputStream in=getAssets().open(asset)){java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);value=bytes.toString("UTF-8");}catch(Exception e){value="License source is available through the project link in About.";}
        TextView t=text(value,13,ink,false);t.setTextIsSelectable(true);t.setLineSpacing(dp(3),1);t.setPadding(dp(20),dp(16),dp(20),dp(16));ScrollView scroll=new ScrollView(this);scroll.addView(t);new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("Done",null).show();}
    private void showSplash(FrameLayout scene){
        FrameLayout splash=new FrameLayout(this);splash.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF131A36,0xFF263769,0xFF161C38}));splash.setClickable(true);scene.addView(splash,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout center=column();center.setGravity(Gravity.CENTER);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);cp.setMargins(dp(24),0,dp(24),0);splash.addView(center,cp);
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_media);center.addView(icon,new LinearLayout.LayoutParams(dp(100),dp(100)));gap(center,22);center.addView(text("Pocket Media",30,Color.WHITE,true));gap(center,10);center.addView(text("Video. Audio. Yours.",15,0xFFBFCDF4,false));gap(center,28);
        LinearLayout bars=new LinearLayout(this);bars.setGravity(Gravity.CENTER);center.addView(bars,new LinearLayout.LayoutParams(-2,dp(24)));
        java.util.List<android.animation.Animator> animations=new java.util.ArrayList<>();
        for(int j=0;j<5;j++){View bar=new View(this);bar.setBackground(shape(0xFFB7C9FF,3));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(4),dp(20));lp.setMargins(dp(3),0,dp(3),0);bars.addView(bar,lp);android.animation.ObjectAnimator a=android.animation.ObjectAnimator.ofFloat(bar,View.SCALE_Y,0.3f,1f,0.3f);a.setDuration(650);a.setStartDelay(j*90L);a.setRepeatCount(android.animation.ValueAnimator.INFINITE);animations.add(a);}
        if(android.animation.ValueAnimator.areAnimatorsEnabled()){splashAnimation=new android.animation.AnimatorSet();splashAnimation.playTogether(animations);splashAnimation.start();icon.setScaleX(.8f);icon.setScaleY(.8f);icon.setAlpha(0);icon.animate().scaleX(1).scaleY(1).alpha(1).setDuration(450).start();}
        handler.postDelayed(()->{if(isFinishing()||isDestroyed())return;if(splashAnimation!=null)splashAnimation.cancel();if(android.animation.ValueAnimator.areAnimatorsEnabled())splash.animate().alpha(0).setDuration(220).withEndAction(()->scene.removeView(splash)).start();else scene.removeView(splash);},1600);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=61||result!=RESULT_OK||data==null||data.getData()==null)return;
        if(ConvertService.busy||importingSession){Toast.makeText(this,"Finish the current task first.",Toast.LENGTH_SHORT).show();return;}
        importingSession=true;sessionStatus.setText("Importing session…");Uri uri=data.getData();new Thread(()->{String message;try{PlatformSession.importFile(this,uri);message="Session saved on this phone only.";}catch(Exception e){message=e.getMessage()==null?"Could not import session.":e.getMessage();}String display=message;runOnUiThread(()->{importingSession=false;if(!isDestroyed())sessionStatus.setText(display);});},"session-import").start();
    }
    protected void onDestroy(){if(splashAnimation!=null)splashAnimation.cancel();handler.removeCallbacksAndMessages(null);super.onDestroy();}
    protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);receiveSharedLink(i);}
    private void receiveSharedLink(Intent i){if(Intent.ACTION_SEND.equals(i.getAction())&&i.hasExtra(Intent.EXTRA_TEXT)){if(ConvertService.busy)Toast.makeText(this,"Finish the current download, then share the next link.",Toast.LENGTH_SHORT).show();else{link.setText(i.getStringExtra(Intent.EXTRA_TEXT));selectTab(0,false);}}}
    protected void onResume(){super.onResume();handler.post(refresh);}
    protected void onPause(){handler.removeCallbacks(refresh);getSharedPreferences("draft",0).edit().putString("link",link.getText().toString()).putInt("mode",mode.getSelectedItemPosition()).putInt("quality",quality.getSelectedItemPosition()).apply();super.onPause();}
    protected void onSaveInstanceState(Bundle b){b.putInt("mode",mode.getSelectedItemPosition());b.putInt("quality",quality.getSelectedItemPosition());b.putInt("tab",selectedTab);b.putString("link",link.getText().toString());super.onSaveInstanceState(b);}
    @Override public void onBackPressed(){if(selectedTab!=0)selectTab(0,true);else super.onBackPressed();}
    private static class LiquidBackground extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        LiquidBackground(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        protected void onDraw(Canvas c){paint.setShader(null);paint.setFilterBitmap(true);c.drawBitmap(GlassMaterial.environment(),null,new RectF(0,0,getWidth(),getHeight()),paint);}
    }
}
