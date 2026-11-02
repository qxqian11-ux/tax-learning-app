"""Create a clean source ZIP; never include signing material, local data or caches."""
from pathlib import Path

# Preserve the historical 1.0.0 delivery filenames.
import re as _version_re
_build_text = (Path(__file__).resolve().parents[1] / 'app/build.gradle').read_text('utf-8-sig')
if _version_re.search(r"versionName\s+'([^']+)'", _build_text).group(1) != '1.0.0':
    raise SystemExit('This is the historical 1.0.0 delivery script. Use scripts/package_source_current.py for the current version.')
import zipfile
import os

root = Path(__file__).resolve().parents[1]
output = root / 'output'
output.mkdir(exist_ok=True)
excluded = {'.git', '.idea', '.gradle', '.gradle-home', '.tools', '.gh-tools', '.gh-config', 'build', 'output', 'signing', 'backup', '__pycache__', '.android'}
with zipfile.ZipFile(output / '税知账-1.0.0-完整源码.zip', 'w', zipfile.ZIP_DEFLATED) as z:
    for folder, dirs, files in os.walk(root):
        dirs[:] = sorted(d for d in dirs if d not in excluded)
        for name in sorted(files):
            p = Path(folder) / name
            rel = p.relative_to(root)
            if p.name not in {'local.properties', 'build.log', 'emulator.log'} and p.suffix not in {'.p12', '.jks', '.keystore', '.lock', '.log'}:
                z.write(p, 'PersonalTaxLearningApp/' + rel.as_posix())
print(output / '税知账-1.0.0-完整源码.zip')
