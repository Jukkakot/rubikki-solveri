"""Compare two folders of screenshots pixel by pixel.

Usage: python scripts/compare-screens.py <baseline-dir> <new-dir> [--limit 0.5]

For every PNG in the baseline, prints the share of pixels whose largest channel difference is
over 8. Fails (exit 1) when an image is missing, changes size, or differs in more than the limit
(per cent). Used to check that moving the UI to the shared module left Android unchanged.
Needs Pillow (pip install pillow).
"""
import sys
from pathlib import Path

from PIL import Image, ImageChops

THRESHOLD = 8


def differing_share(a: Image.Image, b: Image.Image) -> float:
    diff = ImageChops.difference(a.convert("RGB"), b.convert("RGB"))
    channels = diff.split()
    most = ImageChops.lighter(ImageChops.lighter(channels[0], channels[1]), channels[2])
    over = sum(most.point(lambda v: 255 if v > THRESHOLD else 0).histogram()[255:])
    return 100.0 * over / (a.width * a.height)


def main() -> int:
    args = sys.argv[1:]
    limit = 0.5
    if "--limit" in args:
        i = args.index("--limit")
        limit = float(args[i + 1])
        del args[i:i + 2]
    if len(args) != 2:
        print(__doc__)
        return 2
    base, new = Path(args[0]), Path(args[1])
    failed = 0
    rows = []
    for old in sorted(base.rglob("*.png")):
        rel = old.relative_to(base)
        other = new / rel
        if not other.exists():
            rows.append((str(rel), "missing"))
            failed += 1
            continue
        a, b = Image.open(old), Image.open(other)
        if a.size != b.size:
            rows.append((str(rel), f"size {a.size} -> {b.size}"))
            failed += 1
            continue
        share = differing_share(a, b)
        bad = share > limit
        failed += bad
        rows.append((str(rel), f"{share:6.2f} %" + ("  FAIL" if bad else "")))
    width = max((len(r[0]) for r in rows), default=10)
    for name, result in rows:
        print(f"{name:<{width}}  {result}")
    print(f"{len(rows)} images, {failed} over {limit} %")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
