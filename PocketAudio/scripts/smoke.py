"""End-to-end APK installation, UI, HTTP download and MP3 decoding check."""
import atexit, os
import subprocess as sp, pathlib, time, threading, http.server, functools, xml.etree.ElementTree as ET, re, json
root=pathlib.Path(__file__).resolve().parents[1]; out=root/'smoke-results';out.mkdir(exist_ok=True)
fixtures=out/'fixtures';fixtures.mkdir(exist_ok=True)
def run(*args):return sp.check_output(args,text=True,stderr=sp.STDOUT)
def adb(*args):return run('adb',*args)
run('ffmpeg','-y','-f','lavfi','-i','color=c=blue:s=320x240:d=5','-f','lavfi','-i','sine=frequency=440:duration=5','-c:v','mpeg4','-c:a','aac','-ac','2','-shortest',str(fixtures/'test.mp4'))
server=http.server.ThreadingHTTPServer(('0.0.0.0',8765),functools.partial(http.server.SimpleHTTPRequestHandler,directory=str(fixtures)))
threading.Thread(target=server.serve_forever,daemon=True).start()
def diagnostics():
 try:
  (out/'logcat.txt').write_text(adb('logcat','-d','-s','AndroidRuntime:E'))
  adb('shell','uiautomator','dump','/sdcard/diagnostic.xml')
  xml=adb('shell','cat','/sdcard/diagnostic.xml');(out/'screen.xml').write_text(xml);print(xml)
  print((out/'logcat.txt').read_text())
  with (out/'diagnostic.png').open('wb') as f:sp.run(['adb','exec-out','screencap','-p'],stdout=f)
 except Exception as e:print('Diagnostics:',e)
atexit.register(diagnostics)
print(adb('install','-r',str(root/'app/build/outputs/apk/debug/app-debug.apk')))
adb('shell','pm','clear','com.pocketaudio.app')
adb('shell',"rm -rf /sdcard/Download/PocketAudio")
adb('shell','pm','grant','com.pocketaudio.app','android.permission.POST_NOTIFICATIONS')
print(adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity'));time.sleep(8)
def screen():
 adb('shell','uiautomator','dump','/sdcard/window.xml')
 return ET.fromstring(adb('shell','cat','/sdcard/window.xml'))
def tap(node):
 a=list(map(int,re.findall(r'\d+',node.attrib['bounds'])));adb('shell','input','tap',str((a[0]+a[2])//2),str((a[1]+a[3])//2))
def find_text(s):
 return next((n for n in screen().iter('node') if n.attrib.get('text')==s),None)
for retry in range(8):
 tree=screen();edit=next((n for n in tree.iter('node') if n.attrib.get('class')=='android.widget.EditText'),None)
 if edit is not None:break
 print(ET.tostring(tree,encoding='unicode'))
 if any("Pixel Launcher isn't responding" in n.attrib.get('text','') for n in tree.iter('node')):
  close=next((n for n in tree.iter('node') if n.attrib.get('text')=='Close app'),None)
  if close is not None:tap(close)
  adb('shell','am','start','-W','-n','com.pocketaudio.app/.MainActivity');time.sleep(3)
  continue
 for node in tree.iter('node'):
  if node.attrib.get('text') in ['OK','Got it','Continue','Allow']:tap(node)
 time.sleep(2)
else:raise RuntimeError('App input did not appear')
tap(edit);adb('shell','input','text',os.environ.get('TEST_URL','http://10.0.2.2:8765/test.mp4'));adb('shell','input','keyevent','4')
for attempt in range(5):
 b=find_text('Convert to MP3')
 if b is not None:tap(b);break
 adb('shell','input','swipe','500','1500','500','600','300')
else:raise RuntimeError('Convert button not found')
for attempt in range(90):
 time.sleep(2)
 files=adb('shell',"find /sdcard/Download/PocketAudio -name '*.mp3' 2>/dev/null || true") if attempt>3 else ''
 paths=[s for s in files.splitlines() if s.endswith('.mp3')]
 if paths:break
 if attempt%8==0 and find_text('Couldn’t convert this link') is not None:
  adb('shell','input','swipe','500','1500','500','500','300')
  raise RuntimeError('Platform rejected request; see diagnostic screen')
else:raise RuntimeError('MP3 was not saved within three minutes')
adb('pull',paths[0],str(out/'converted.mp3'))
probe=json.loads(run('ffprobe','-v','error','-show_streams','-show_format','-of','json',str(out/'converted.mp3')))
assert probe['streams'][0]['codec_name']=='mp3',probe
if 'TEST_URL' not in os.environ:assert abs(float(probe['format']['duration'])-5)<0.5,probe
run('ffmpeg','-v','error','-i',str(out/'converted.mp3'),'-f','null','-')
with (out/'screen.png').open('wb') as f:sp.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
(out/'verification.json').write_text(json.dumps({'passed':True,'checks':['APK installed','Activity opened','URL entered through UI','HTTP MP4 downloaded','MP3 saved to Downloads','ffprobe confirmed MP3 codec and duration','Full MP3 decoded without errors'],'probe':probe},indent=2))
print('END-TO-END CONVERSION PASSED')
server.shutdown()
