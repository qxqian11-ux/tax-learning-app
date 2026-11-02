"""Publish only an APK to an existing GitHub repository; secrets never enter the package."""
import argparse
import hashlib
import json
import os
import re
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import quote
from urllib.request import Request, urlopen
from make_qr import create_qr, verify_apk


def publish(repo, apk, output):
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repo):
        raise ValueError('仓库格式须为 用户名/仓库名')
    token = os.environ.get('GITHUB_TOKEN')
    if not token:
        raise ValueError('缺少 GITHUB_TOKEN，请在终端环境中提供，不要写入源代码或聊天')
    headers = {'Authorization': f'Bearer {token}', 'Accept': 'application/vnd.github+json', 'X-GitHub-Api-Version': '2022-11-28', 'User-Agent': 'TaxLearning-Release/1.0'}
    api = f'https://api.github.com/repos/{repo}'

    def call(url, body=None, binary=False):
        h = dict(headers)
        if body is not None:
            h['Content-Type'] = 'application/vnd.android.package-archive' if binary else 'application/json'
            data = body if binary else json.dumps(body).encode('utf-8')
        else:
            data = None
        with urlopen(Request(url, data=data, headers=h), timeout=120) as r:
            return json.load(r)

    metadata = call(api)
    if metadata.get('private'):
        raise ValueError('该仓库为私有，老师同学无法直接下载。请指定用于公开课程 APK 的公共仓库；脚本不会修改仓库权限。')
    try:
        release = call(api + '/releases/tags/v1.0.0')
    except HTTPError as e:
        if e.code != 404:
            raise
        release = call(api + '/releases', {'tag_name': 'v1.0.0', 'name': '税知账 · 学习版 v1.0.0', 'body': '独立课程学习应用，仅使用模拟资料，不属于官方税务应用。Android 7.0+；HarmonyOS 4.2.0 需实机验收；不适用于 HarmonyOS NEXT。', 'draft': False, 'prerelease': False})
    apk = Path(apk)
    data = apk.read_bytes()
    asset_name = 'tax-learning-1.0.0-release.apk'
    existing = next((x for x in release.get('assets', []) if x['name'] == asset_name), None)
    if existing:
        verify_apk(existing['browser_download_url'], apk)  # refuses to overwrite a different file
        asset = existing
    else:
        asset = call(release['upload_url'].split('{')[0] + '?name=' + quote(asset_name), data, True)
    create_qr(asset['browser_download_url'], apk, output)
    print('Release：' + release['html_url'])
    print('APK：' + asset['browser_download_url'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--repo', required=True)
    parser.add_argument('--apk', required=True)
    parser.add_argument('--out', default='output')
    args = parser.parse_args()
    try:
        publish(args.repo, args.apk, args.out)
    except HTTPError as e:
        raise SystemExit(f'GitHub 请求失败 HTTP {e.code}；请检查仓库、网络与账户权限。')
