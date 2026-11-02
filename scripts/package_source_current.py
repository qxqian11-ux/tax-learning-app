"""Export versioned source without credentials, build caches or APK outputs."""
from pathlib import Path
import re, zipfile
root=Path(__file__).resolve().parents[1]
version=re.search(r"versionName\s+'([^']+)'",(root/'app/build.gradle').read_text('utf-8-sig')).group(1)
out=root/'output'/('v'+version);out.mkdir(parents=True,exist_ok=True)
files=[]
for name in ['app/src','app/schemas','assets','docs','gradle','scripts','acceptance/probe/src']:
    files += [p for p in (root/name).rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix!='.pyc']
files += [root/name for name in ['README.md','.gitignore','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','app/build.gradle','app/proguard-rules.pro','acceptance/build.gradle','acceptance/settings.gradle','acceptance/gradle.properties','acceptance/probe/build.gradle']]
path=out/('tax-learning-'+version+'-source.zip')
with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(set(files)):
        if p.exists():z.write(p,p.relative_to(root).as_posix())
print(path)
