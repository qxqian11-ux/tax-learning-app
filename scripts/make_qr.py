"""Generate a QR only after downloading and verifying the actual APK URL."""
import argparse
import hashlib
import json
import sys
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.parse import urlparse


def verify_apk(url, apk):
    if urlparse(url).scheme != 'https':
        raise ValueError('请使用已发布的真实 HTTPS APK 下载地址')
    expected = hashlib.sha256(Path(apk).read_bytes()).hexdigest()
    digest = hashlib.sha256()
    count = 0
    with urlopen(Request(url, headers={'User-Agent': 'TaxLearning-Release-Verify/1.0'}), timeout=60) as response:
        while chunk := response.read(128 * 1024):
            count += len(chunk)
            if count > 100 * 1024 * 1024:
                raise ValueError('下载内容超出 100 MB，停止校验')
            digest.update(chunk)
    if digest.hexdigest() != expected:
        raise ValueError('远程内容与本机 APK 不一致；可能是网页、错误文件或未完成上传。未生成二维码。')
    return expected, count


def create_qr(url, apk, output):
    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / '.tools'))
    import qrcode
    digest, count = verify_apk(url, apk)
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=12, border=4)
    qr.add_data(url)
    qr.make(fit=True)
    qr.make_image(fill_color='#252936', back_color='white').save(output / 'APK下载二维码.png')
    (output / 'download.json').write_text(json.dumps({'url': url, 'sha256': digest, 'bytes': count}, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'下载内容已校验，二维码：{output / "APK下载二维码.png"}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--url', required=True)
    parser.add_argument('--apk', required=True)
    parser.add_argument('--out', default='output')
    args = parser.parse_args()
    create_qr(args.url, args.apk, args.out)
