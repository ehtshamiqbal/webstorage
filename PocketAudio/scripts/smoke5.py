import pathlib,subprocess as sp,time,threading,http.server,functools,xml.etree.ElementTree as ET,re,json,atexit
root=pathlib.Path(__file__).resolve().parents[1];out=root/'smoke-results';out.mkdir(exist_ok=True);fixtures=out/'fixtures';fixtures.mkdir(exist_ok=True)
def run(*args):return sp.check_output(args,text=True,stderr=sp.STDOUT)
def adb(*args):return run('adb',*args)
def screen():
 adb('shell','uiautomator','dump','/sdcard/screen.xml');return ET.fromstring(adb('shell','cat','/sdcard/screen.xml'))
def shot(name):
 with (out/(name+'.png')).open('wb') as f:sp.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
def tap(n):
 a=list(map(int,re.findall(r'\d+',n.attrib['bounds'])));adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2))
def seek(text):
 for attempt in range(5):
  n=next((n for n in screen().iter('node') if n.attrib.get('text')==text),None)
  if n is not None:return n
  adb('shell','input','swipe','500','1450','500','550','200')
 raise RuntimeError('Not found: '+text)
def diagnostics():
 try:
  print(adb('logcat','-d','-s','AndroidRuntime:E'));print(ET.tostring(screen(),encoding='unicode'));shot('screen')
 except Exception as e:print(e)
atexit.register(diagnostics)
first_request=threading.Event()
class MediaHandler(http.server.SimpleHTTPRequestHandler):
 def do_GET(self):
  first_request.set()
  if self.path.startswith('/slow.mp4'):time.sleep(12);self.path='/test.mp4'
  try:super().do_GET()
  except (BrokenPipeError,ConnectionResetError):pass
server=http.server.ThreadingHTTPServer(('0.0.0.0',8765),functools.partial(MediaHandler,directory=str(fixtures)));threading.Thread(target=server.serve_forever,daemon=True).start()
print(adb('install','-r',str(root/'app/build/outputs/apk/release/app-x86_64-release.apk')))
adb('shell','pm','clear','com.pocketaudio.app');adb('shell','pm','grant','com.pocketaudio.app','android.permission.POST_NOTIFICATIONS')
print(adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity'));time.sleep(4)
for retry in range(8):
 tree=screen();edit=next((n for n in tree.iter('node') if n.attrib.get('resource-id','').endswith('/link_input')),None)
 if edit is not None:break
 if any("Pixel Launcher isn't responding" in n.attrib.get('text','') for n in tree.iter('node')):
  tap(next(n for n in tree.iter('node') if n.attrib.get('text')=='Close app'));adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity')
 time.sleep(2)
else:raise RuntimeError('Input missing')
shot('home');assert not any(n.attrib.get('resource-id','').endswith('/name_input') for n in screen().iter('node'))
# Separate screens; the download form must not leak into Recent or Settings.
tap(seek('Recent'));seek('Your first save starts here');assert not any(n.attrib.get('resource-id','').endswith('/link_input') for n in screen().iter('node'));shot('recent-empty')
tap(seek('Settings'));seek('Update video support');shot('settings');tap(seek('Download'))
results=[]
for label,width,height,fps in [('audio',320,240,24),('1080p60',1920,1080,60),('VP9',640,360,24),('AV1',640,360,24)]:
 video=label!='audio'
 codec='libvpx-vp9' if label=='VP9' else 'libaom-av1' if label=='AV1' else 'libx264'
 extension='webm' if label in ('VP9','AV1') else 'mp4'
 source=fixtures/('source-'+label+'.'+extension)
 args=['ffmpeg','-y','-f','lavfi','-i',f'color=c=blue:s={width}x{height}:r={fps}:d=1','-f','lavfi','-i','sine=frequency=440:duration=1','-c:v',codec,'-threads','1']
 args+=['-preset','ultrafast'] if codec=='libx264' else ['-cpu-used','8']
 args+=['-pix_fmt','yuv420p','-c:a','libopus' if extension=='webm' else 'aac','-ac','2','-shortest',str(source)]
 run(*args)
 title='Night Drive '+label
 (fixtures/(label+'.html')).write_text('<html><head><title>'+title+'</title><meta property="og:title" content="'+title+'"></head><body><video controls src="'+source.name+'"></video></body></html>')
 edit=next(n for n in screen().iter('node') if n.attrib.get('resource-id','').endswith('/link_input'))
 tap(edit);adb('shell','input','keyevent','KEYCODE_MOVE_END');adb('shell','input','keyevent','--longpress','KEYCODE_DEL')
 # Select all reliably using UI input key combination, then replace text.
 adb('shell','input','keycombination','113','29');adb('shell','input','text','http://10.0.2.2:8765/'+label+'.html');adb('shell','input','keyevent','4')
 if label=='1080p60':
  tap(seek('Audio · MP3'));tap(seek('Video · MP4'));tap(seek('1080p · Gallery safe'));tap(seek('1080p60 · Gallery safe'))
 first_request.clear();start=time.monotonic();tap(seek('Download MP4' if video else 'Download MP3'))
 tree=screen();assert any(n.attrib.get('text') in ('Working…','Saved · View in Recent') for n in tree.iter('node')),ET.tostring(tree)
 assert first_request.wait(40),'Extractor did not contact host promptly';request_seconds=time.monotonic()-start
 shot('active-'+label)
 tap(seek('Recent'));seek('Your collection');tap(seek('Download'))
 folder='/sdcard/Movies/PocketMedia' if video else '/sdcard/Music/PocketMedia'
 for retry in range(90):
  time.sleep(2);paths=[x for x in adb('shell',f'find {folder} -type f 2>/dev/null || true').splitlines() if x.endswith('.mp4' if video else '.mp3') and title in x]
  if paths:break
 else:raise RuntimeError('No output for '+label)
 dest=out/(label+('.mp4' if video else '.mp3'));adb('pull',paths[0],str(dest));probe=json.loads(run('ffprobe','-v','error','-show_streams','-of','json',str(dest)))
 assert any(s['codec_type']=='audio' for s in probe['streams'])
 if video:
  v=next(s for s in probe['streams'] if s['codec_type']=='video');assert v['width']==width and v['height']==height and v['codec_name']=='h264' and v['pix_fmt']=='yuv420p',probe
  assert any(s['codec_name']=='aac' for s in probe['streams']),probe
  if label=='1080p60':assert v['r_frame_rate']=='60/1',probe
 else:assert any(s['codec_name']=='mp3' for s in probe['streams'])
 run('ffmpeg','-v','error','-threads','1','-i',str(dest),'-f','null','-')
 results.append({'case':label,'passed':True,'time_to_host_seconds_including_ui_dump':round(request_seconds,2),'streams':probe['streams']});print('PASS',label,request_seconds,flush=True)
 time.sleep(2)
tap(seek('Recent'));seek('Your collection');seek('Open');shot('recent-saved')
tap(seek('Download'));seek('1080p60 · Gallery safe')
# Restore the selected quality and draft after process restart.
adb('shell','input','keyevent','3');time.sleep(1);adb('shell','am','force-stop','com.pocketaudio.app');adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity');time.sleep(2);seek('1080p60 · Gallery safe');seek('http://10.0.2.2:8765/AV1.html');shot('home-final')
# Verify actual Android decoding, beyond a desktop FFmpeg codec check.
adb('install','-r',str(root/'app/build/outputs/apk/androidTest/release/app-release-androidTest.apk'))
playback=adb('shell','am','instrument','-w','com.pocketaudio.app.test/com.pocketaudio.app.PlaybackChecks');print(playback);assert 'ANDROID_VISIBLE_FRAMES_PASS count=3' in playback,playback
# Structured About screen.
adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity');time.sleep(2);tap(seek('Settings'));tap(seek('About & open-source licenses'));seek('How it works');shot('about');adb('shell','input','keyevent','4');tap(seek('Download'))
results.append({'case':'separate_tabs_draft_restore_about_android_playback','passed':True});(out/'verification.json').write_text(json.dumps(results,indent=2));print('ALL V7 TESTS PASSED',flush=True)
