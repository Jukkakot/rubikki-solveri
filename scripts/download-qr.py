"""Writes docs/img/download-qr.png: a QR code for the fixed download address of the latest APK.

The address never changes, so the image is committed; rerun only if the address changes.
Needs: pip install "qrcode[pil]"
"""
from pathlib import Path

import qrcode

URL = "https://github.com/Jukkakot/rubikki-solveri/releases/latest/download/rubikki-solveri.apk"

out = Path(__file__).resolve().parent.parent / "docs" / "img" / "download-qr.png"
image = qrcode.make(URL, box_size=8, border=3)
image.save(out)
print(out)
