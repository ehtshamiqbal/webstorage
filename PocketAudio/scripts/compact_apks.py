"""Lossless APK compression; remove only unused static link archives, never runtime codecs."""
import io,zipfile,pathlib,subprocess as sp,os,json
root=pathlib.Path(__file__).resolve().parents[1]
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0'
reports=[]
for apk in sorted((root/'app/build/outputs/apk/release').glob('*.apk')):
    before=apk.stat().st_size;stage=apk.with_suffix('.compact');removed=[]
    with zipfile.ZipFile(apk) as src,zipfile.ZipFile(stage,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as dst:
        for info in src.infolist():
            if info.filename.startswith('META-INF/') and info.filename.upper().endswith(('.RSA','.DSA','.EC','.SF','MANIFEST.MF')):continue
            data=src.read(info)
            if info.filename.startswith('lib/') and info.filename.endswith('.zip.so'):
                result=io.BytesIO()
                with zipfile.ZipFile(io.BytesIO(data)) as package,zipfile.ZipFile(result,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as packed:
                    for entry in package.infolist():
                        if entry.filename.endswith('.a'):
                            removed.append({'path':info.filename+'/'+entry.filename,'bytes':entry.file_size});continue
                        # Preserve Unix mode, including symbolic links, and exact payload bytes.
                        payload=package.read(entry)
                        entry.compress_type=zipfile.ZIP_DEFLATED
                        packed.writestr(entry,payload,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
                data=result.getvalue()
                with zipfile.ZipFile(io.BytesIO(src.read(info))) as old,zipfile.ZipFile(io.BytesIO(data)) as new:
                    assert all(old.read(n)==new.read(n) for n in new.namelist()),'Runtime payload changed'
            info.compress_type=zipfile.ZIP_STORED if info.filename=='resources.arsc' else zipfile.ZIP_DEFLATED
            dst.writestr(info,data,compress_type=info.compress_type,compresslevel=9)
    aligned=apk.with_suffix('.aligned')
    sp.run([str(sdk/'zipalign'),'-f','-P','16','4',str(stage),str(aligned)],check=True)
    sp.run([str(sdk/'apksigner'),'sign','--ks',str(pathlib.Path.home()/'.android/debug.keystore'),'--ks-pass','pass:android','--key-pass','pass:android','--out',str(apk),str(aligned)],check=True)
    sp.run([str(sdk/'apksigner'),'verify',str(apk)],check=True)
    stage.unlink();aligned.unlink()
    reports.append({'apk':apk.name,'before_bytes':before,'after_bytes':apk.stat().st_size,'removed_static_archives':removed})
(root/'size-report.json').write_text(json.dumps(reports,indent=2))
print(json.dumps(reports,indent=2))
