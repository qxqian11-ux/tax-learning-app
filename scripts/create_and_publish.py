"""Use an already authorized Git Credential Manager account without displaying secrets."""
import argparse
import json
import os
import subprocess
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from publish_release import publish
import sys


def run(apk, output, token=None):
    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / '.tools'))
    import qrcode  # Verify the QR dependency before creating any remote resources.
    # GCM holds the OAuth token in the OS credential store. It stays in memory here.
    if token is None:
        env = dict(os.environ, GCM_INTERACTIVE='never')
        credential = subprocess.run(['git', 'credential-manager', 'get'], input='protocol=https\nhost=github.com\n\n', capture_output=True, text=True, env=env)
        if credential.returncode:
            raise ValueError('GitHub 尚未完成 Git Credential Manager 授权')
        values = dict(line.split('=', 1) for line in credential.stdout.splitlines() if '=' in line)
        token = values.get('password')
    if not token:
        raise ValueError('没有可用的 GitHub 授权')
    headers = {'Authorization': f'Bearer {token}', 'Accept': 'application/vnd.github+json', 'X-GitHub-Api-Version': '2022-11-28', 'User-Agent': 'TaxLearning-Release/1.0'}

    def call(path, data=None):
        h = dict(headers)
        if data is not None:
            h['Content-Type'] = 'application/json'
        with urlopen(Request('https://api.github.com' + path, headers=h, data=None if data is None else json.dumps(data).encode('utf-8')), timeout=60) as r:
            return json.load(r)

    account = call('/user')['login']
    repo = account + '/tax-learning-app'
    try:
        existing = call('/repos/' + repo)
        if existing.get('description') != '税知账 · 个人所得税学习版 APK 下载（独立课程项目，仅模拟数据）':
            raise ValueError('同名仓库已经存在且不是本工具创建的课程仓库，请指定其他仓库名称后再发布。')
    except HTTPError as e:
        if e.code != 404:
            raise
        call('/user/repos', {'name': 'tax-learning-app', 'description': '税知账 · 个人所得税学习版 APK 下载（独立课程项目，仅模拟数据）', 'private': False, 'auto_init': True})
    os.environ['GITHUB_TOKEN'] = token
    try:
        publish(repo, apk, output)
    finally:
        os.environ.pop('GITHUB_TOKEN', None)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', required=True)
    parser.add_argument('--out', default='output')
    args = parser.parse_args()
    try:
        run(args.apk, args.out)
    except HTTPError as e:
        raise SystemExit(f'GitHub 请求失败 HTTP {e.code}，未显示任何账户令牌。')
    except ValueError as e:
        raise SystemExit(str(e))
