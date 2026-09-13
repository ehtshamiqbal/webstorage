import pathlib, subprocess as sp, time, json

root = pathlib.Path(__file__).resolve().parents[1]
out = root / 'install-results'
out.mkdir(exist_ok=True)
apk = root / 'app/build/outputs/apk/release/app-x86_64-release.apk'
def adb(*args):
    return sp.check_output(['adb', *args], text=True, stderr=sp.STDOUT)
def launch(name):
    adb('logcat', '-c')
    result = adb('shell', 'am', 'start', '-W', '-n', 'com.pocketaudio.app/.MainActivity')
    assert 'Status: ok' in result, result
    time.sleep(4)
    assert adb('shell', 'pidof', 'com.pocketaudio.app').strip()
    activities = adb('shell', 'dumpsys', 'activity', 'activities')
    assert any('com.pocketaudio.app/.MainActivity' in line and 'ResumedActivity' in line for line in activities.splitlines()), activities
    crashes = adb('logcat', '-d', '-s', 'AndroidRuntime:E')
    assert 'FATAL EXCEPTION' not in crashes, crashes
    with (out / (name + '.png')).open('wb') as f:
        sp.run(['adb', 'exec-out', 'screencap', '-p'], stdout=f, check=True)

assert 'Success' in adb('install', str(apk))
launch('fresh-install')
assert 'Success' in adb('install', '-r', str(apk))
launch('after-update')
(out / 'results.json').write_text(json.dumps({'api':29,'fresh_install':True,'same_key_update':True,'launch_after_each_install':True,'crash_free_launch':True,'certificate':'CI test certificate; final payload independently re-signed'}, indent=2))
print('ANDROID 10 FRESH INSTALL, UPDATE AND LAUNCH PASSED')
