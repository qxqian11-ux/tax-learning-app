"""Collect our own debug instrumentation screenshots using the debug app identity."""
from pathlib import Path
import subprocess
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--adb', default=r'D:\tax_project\Android SDK\platform-tools\adb.exe')
parser.add_argument('--serial', default='emulator-5554')
parser.add_argument('--out', default='output/screenshots')
args = parser.parse_args()
target = Path(args.out)
target.mkdir(parents=True, exist_ok=True)
for name in ['01-home', '02-query', '03-records', '04-editor', '05-statistics', '06-backup']:
    data = subprocess.check_output([args.adb, '-s', args.serial, 'exec-out', 'run-as', 'cn.taxlearning.ledger.debug', 'cat', f'files/screenshots/{name}.png'])
    if not data.startswith(b'\x89PNG\r\n\x1a\n'):
        raise ValueError('设备返回内容不是有效 PNG')
    (target / f'{name}.png').write_bytes(data)
    print(name)
