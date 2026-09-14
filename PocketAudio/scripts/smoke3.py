import pathlib,subprocess as sp,time,threading,http.server,functools,xml.etree.ElementTree as ET,re,json,atexit,os
root=pathlib.Path(__file__).resolve().parents[1];out=root/'smoke-results';out.mkdir(exist_ok=True);fixtures=out/'fixtures';fixtures.mkdir(exist_ok=True)
def run(*args):return sp.check_output(args,text=True,stderr=sp.STDOUT)
def adb(*args):return run('adb',*args)
def screen():
 adb('shell','uiautomator','dump','/sdcard/screen.xml');return ET.fromstring(adb('shell','cat','/sdcard/screen.xml'))
def tap(n):
 a=list(map(int,re.findall(r'\d+',n.attrib['bounds'])));adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2))
def seek(text):
 for attempt in range(8):
  tree=screen();n=next((n for n in tree.iter('node') if n.attrib.get('text')==text),None)
  if n is not None:return n
  adb('shell','input','swipe','500','1500','500','550','250')
 raise RuntimeError('Not found: '+text)
def diagnostics():
 try:
  print(adb('logcat','-d','-s','AndroidRuntime:E'))
  print(ET.tostring(screen(),encoding='unicode'))
  with (out/'screen.png').open('wb') as f:sp.run(['adb','exec-out','screencap','-p'],stdout=f)
 except Exception as e:print(e)
atexit.register(diagnostics)
server=http.server.ThreadingHTTPServer(('0.0.0.0',8765),functools.partial(http.server.SimpleHTTPRequestHandler,directory=str(fixtures)));threading.Thread(target=server.serve_forever,daemon=True).start()
print(adb('install','-r',str(root/'app/build/outputs/apk/debug/app-debug.apk')))
results=[]
for label,width,height,fps,quality in [('audio',320,240,24,''),('1080p60',1920,1080,60,'1080p · 60 fps preferred'),('4K',3840,2160,2,'4K · Up to 2160p'),('6K',5760,3240,2,'6K · Up to 3240p'),('8K',7680,4320,2,'8K · Up to 4320p')]:
 video=label!='audio'; duration='1' if height<=1080 else '0.5'
 run('ffmpeg','-y','-f','lavfi','-i',f'color=c=blue:s={width}x{height}:r={fps}:d={duration}','-f','lavfi','-i',f'sine=frequency=440:duration={duration}','-c:v','libx264','-preset','ultrafast','-threads','1','-pix_fmt','yuv420p','-c:a','aac','-ac','2','-shortest',str(fixtures/'test.mp4'))
 adb('shell','am','force-stop','com.pocketaudio.app');adb('shell','pm','clear','com.pocketaudio.app');adb('shell','pm','grant','com.pocketaudio.app','android.permission.POST_NOTIFICATIONS')
 folder='/sdcard/Movies/PocketMedia' if video else '/sdcard/Music/PocketMedia';adb('shell','rm','-rf',folder)
 print(adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity'));time.sleep(5)
 for retry in range(8):
  tree=screen();edit=next((n for n in tree.iter('node') if n.attrib.get('class')=='android.widget.EditText'),None)
  if edit is not None:break
  if any("Pixel Launcher isn't responding" in n.attrib.get('text','') for n in tree.iter('node')):
   tap(next(n for n in tree.iter('node') if n.attrib.get('text')=='Close app'));adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity')
  time.sleep(2)
 else:raise RuntimeError('Input missing')
 tap(edit);adb('shell','input','text','http://10.0.2.2:8765/test.mp4');adb('shell','input','keyevent','4')
 if video:
  tap(seek('Audio · MP3'));tap(seek('Video · MP4'));time.sleep(1)
  tap(seek('1080p Full HD'));tap(seek(quality))
 tap(seek('Download MP4' if video else 'Download MP3'))
 for retry in range(90):
  time.sleep(2);files=adb('shell',f"find {folder} -type f 2>/dev/null || true");paths=[x for x in files.splitlines() if x.endswith('.mp4' if video else '.mp3')]
  if paths:break
 else:raise RuntimeError('No output for '+label)
 dest=out/(label+('.mp4' if video else '.mp3'));adb('pull',paths[0],str(dest));probe=json.loads(run('ffprobe','-v','error','-show_streams','-of','json',str(dest)))
 audio=[s for s in probe['streams'] if s['codec_type']=='audio'];assert audio,probe
 if video:
  v=next(s for s in probe['streams'] if s['codec_type']=='video');assert v['width']==width and v['height']==height,probe
  if label=='1080p60':assert v['r_frame_rate']=='60/1',probe
 else:assert audio[0]['codec_name']=='mp3',probe
 run('ffmpeg','-v','error','-threads','1','-i',str(dest),'-f','null','-')
 results.append({'case':label,'passed':True,'streams':probe['streams'],'saved_to':folder});(out/'verification.json').write_text(json.dumps(results,indent=2));print('PASS',label,flush=True)
print('ALL MEDIA TESTS PASSED')
