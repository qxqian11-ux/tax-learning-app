"""Publish via the official CLI's saved authorization, keeping token output in memory."""
from pathlib import Path
import argparse
import os
import subprocess
from create_and_publish import run

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--apk', required=True)
parser.add_argument('--out', default=str(root/'output'))
args = parser.parse_args()
env = dict(os.environ, GH_CONFIG_DIR=str(root/'.gh-config'))
result = subprocess.run([str(root/'.gh-tools/gh.exe'), 'auth', 'token', '--hostname', 'github.com'], capture_output=True, text=True, env=env)
if result.returncode or not result.stdout.strip():
    raise SystemExit('GitHub CLI 尚未完成授权')
try:
    run(args.apk, args.out, token=result.stdout.strip())
except Exception as e:
    # Exceptions from HTTP calls contain URL/status, never the Authorization header.
    raise SystemExit(f'发布未完成：{e}')
