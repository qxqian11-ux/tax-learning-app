"""Fetch the official Windows GitHub CLI release into the requested D-drive tool directory."""
import argparse
import hashlib
import io
from pathlib import Path
import re
from urllib.request import Request, urlopen
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--out', required=True)
args = parser.parse_args()

def fetch(url):
    with urlopen(Request(url, headers={'User-Agent':'TaxLearning-Setup/1.0'}), timeout=90) as r:
        return r.read()

with urlopen(Request('https://github.com/cli/cli/releases/latest', headers={'User-Agent':'TaxLearning-Setup/1.0'}), timeout=45) as r:
    url = r.geturl()
match = re.fullmatch(r'https://github.com/cli/cli/releases/tag/v([0-9]+\.[0-9]+\.[0-9]+)', url)
if not match:
    raise ValueError('无法核实官方 CLI Release 版本')
version = match.group(1)
name = f'gh_{version}_windows_amd64.zip'
base = f'https://github.com/cli/cli/releases/download/v{version}/'
data = fetch(base+name)
checksums = fetch(base+f'gh_{version}_checksums.txt').decode()
expected = next(line.split()[0] for line in checksums.splitlines() if line.split()[-1] == name)
if hashlib.sha256(data).hexdigest() != expected:
    raise ValueError('官方 CLI 下载校验失败')
archive=zipfile.ZipFile(io.BytesIO(data))
files=[x for x in archive.namelist() if x=='bin/gh.exe' or x.endswith('/bin/gh.exe')]
if len(files)!=1:
    raise ValueError('官方 CLI 安装包结构异常')
target=Path(args.out)
target.mkdir(parents=True,exist_ok=True)
(target/'gh.exe').write_bytes(archive.read(files[0]))
print('Official GitHub CLI verified:',version)
