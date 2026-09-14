import pathlib,subprocess as sp,time,threading,http.server,functools,xml.etree.ElementTree as ET,re,json,atexit
root=pathlib.Path(__file__).resolve().parents[1];out=root/'smoke-results';out.mkdir(exist_ok=True);fixtures=out/'install29-fixtures';fixtures.mkdir(exist_ok=True)
def run(*args):return sp.check_output(args,text=True,stderr=sp.STDOUT)
def adb(*args):return run('adb',*args)
def screen():
 adb('shell','uiautomator','dump','/sdcard/screen.xml');return ET.fromstring(adb('shell','cat','/sdcard/screen.xml'))
def shot(name):
 with (out/(name+'.png')).open('wb') as f:sp.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
def tap(n):
 a=list(map(int,re.findall(r'\d+',n.attrib['bounds'])));adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2))
def seek(text):
 for i in range(6):
  n=next((n for n in screen().iter('node') if n.attrib.get('text')==text),None)
  if n is not None:return n
  adb('shell','input','swipe','500','1400','500','500','200')
 raise RuntimeError('Missing '+text)
def diagnostics():
 try:print(adb('logcat','-d','-s','AndroidRuntime:E'));shot('android10-final')
 except Exception as e:print(e)
atexit.register(diagnostics)
run('ffmpeg','-y','-f','lavfi','-i','color=c=blue:s=640x360:r=24:d=1','-f','lavfi','-i','sine=frequency=440:duration=1','-c:v','libx264','-preset','ultrafast','-threads','1','-pix_fmt','yuv420p','-c:a','aac','-shortest',str(fixtures/'sample.mp4'))
server=http.server.ThreadingHTTPServer(('0.0.0.0',8765),functools.partial(http.server.SimpleHTTPRequestHandler,directory=str(fixtures)));threading.Thread(target=server.serve_forever,daemon=True).start()
apk=str(root/'app/build/outputs/apk/release/app-release.apk');assert 'Success' in adb('install',apk)
adb('shell','settings','put','global','animator_duration_scale','1')
adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity');time.sleep(.25);shot('animated-splash-android10');time.sleep(3)
seek('Download MP3');shot('android10-home')
adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity','-a','android.intent.action.SEND','--es','android.intent.extra.TEXT','http://10.0.2.2:8765/sample.mp4');time.sleep(1)
tap(seek('Audio · MP3'));tap(seek('Video · MP4'));tap(seek('Download MP4'))
for i in range(90):
 time.sleep(2)
 files=adb('shell','find /sdcard/Movies/PocketMedia -type f 2>/dev/null || true')
 if any(p.endswith('.mp4') for p in files.splitlines()):break
else:raise RuntimeError('Android 10 conversion failed')
time.sleep(3)
# In-place replacement must retain data; test with the same CI signing certificate.
assert 'Success' in adb('install','-r',apk)
adb('install','-r',str(root/'app/build/outputs/apk/androidTest/release/app-release-androidTest.apk'))
playback=adb('shell','am','instrument','-w','-e','expectedVideos','1','com.pocketaudio.app.test/com.pocketaudio.app.PlaybackChecks');print(playback);assert 'ANDROID_VISIBLE_FRAMES_PASS count=1' in playback
(out/'android10-install.json').write_text(json.dumps({'android_api':29,'fresh_install':True,'same_certificate_update':True,'media_and_history_retained':True,'native_video_frame_decode':True},indent=2))
print('ANDROID 10 RELEASE INSTALL AND UPDATE PASSED',flush=True)
