"""Build the shareable delivery folder; signing and account credentials stay private."""
from pathlib import Path

# Preserve the historical 1.0.0 delivery filenames.
import re as _version_re
_build_text = (Path(__file__).resolve().parents[1] / 'app/build.gradle').read_text('utf-8-sig')
if _version_re.search(r"versionName\s+'([^']+)'", _build_text).group(1) != '1.0.0':
    raise SystemExit('This is the historical 1.0.0 delivery script. Use scripts/package_source_current.py for the current version.')
import hashlib
import json
import shutil
import subprocess
import sys
import zipfile

root=Path(__file__).resolve().parents[1]
out=root/'output'
out.mkdir(exist_ok=True)
for name in ['手机安装说明.md','签名与图标.md','验证结果.md','交付清单.md']:
    shutil.copy2(root/'docs'/name,out/name)
reports=out/'verification'
reports.mkdir(exist_ok=True)
for source,name in [
    (root/'app/build/test-results/testDebugUnitTest/TEST-cn.taxlearning.ledger.RulesTest.xml','unit-tests.xml'),
    (root/'app/build/reports/lint-results-debug.txt','android-lint.txt'),
]:
    if source.exists():shutil.copy2(source,reports/name)
device=list((root/'app/build/outputs/androidTest-results/connected/debug').glob('*.xml'))
if device:shutil.copy2(device[0],reports/'device-tests.xml')
subprocess.run([sys.executable,str(root/'scripts/package_source.py')],check=True)
source_zip=next(out.glob('*完整源码.zip'))
with zipfile.ZipFile(source_zip) as z:
    forbidden={'signing','.gh-config','.gh-tools','.tools','.gradle-home','.android','build'}
    if any(forbidden.intersection(Path(name).parts) for name in z.namelist()):
        raise ValueError('源码包包含不应分享的目录，停止交付')
apk=out/'税知账-1.0.0-release.apk'
metadata={'applicationId':'cn.taxlearning.ledger','versionName':'1.0.0','versionCode':1,'minSdk':24,'targetSdk':34,
    'releaseApk':apk.name,'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
    'certificateSha256':'3ed3b8afba8e782aa439bbe99b3ca3f5ae41e1d3c14da479c805455364613d18'}
(out/'version.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2),encoding='utf-8')
files=sorted(x for x in out.rglob('*') if x.is_file() and x.name!='SHA256SUMS.txt')
(out/'SHA256SUMS.txt').write_text('\n'.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.relative_to(out).as_posix() for p in files)+'\n',encoding='utf-8')
print('Delivery ready:',out)
