import pathlib,urllib.request,hashlib
root=pathlib.Path(__file__).resolve().parents[1]
url='https://github.com/yt-dlp/yt-dlp/releases/download/2026.08.19/yt-dlp'
expected='1fa6733c37ea6fb51c99ad8fe785e7b7e5f3246c9b980230329d4fb72ed8d4d6'
payload=urllib.request.urlopen(url,timeout=90).read()
assert hashlib.sha256(payload).hexdigest()==expected,'Engine checksum mismatch'
target=root/'app/src/main/res/raw/ytdlp';target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(payload)
print('Verified bundled engine 2026.08.19')
