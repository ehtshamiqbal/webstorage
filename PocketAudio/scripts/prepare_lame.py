"""Fetch the versioned upstream LAME source and preserve its license."""
import pathlib, tarfile, urllib.request, shutil
root=pathlib.Path(__file__).resolve().parents[1]
dest=root/'third_party'; dest.mkdir(exist_ok=True)
if not (dest/'lame-3.100/configure').exists():
    archive=dest/'lame-3.100.tar.gz'
    urllib.request.urlretrieve('https://downloads.sourceforge.net/project/lame/lame/3.100/lame-3.100.tar.gz',archive)
    with tarfile.open(archive) as tf:
        tf.extractall(dest, filter='data')
assets=root/'app/src/main/assets'; assets.mkdir(parents=True,exist_ok=True)
shutil.copyfile(dest/'lame-3.100/COPYING',assets/'LAME-LICENSE.txt')
print('LAME 3.100 source and license ready.')
