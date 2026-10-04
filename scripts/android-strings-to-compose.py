"""Convert the Android string resources to Compose Multiplatform resources (one-off, kept for redo).

Usage: python scripts/android-strings-to-compose.py

Reads app/src/main/res/values{,-en}/strings.xml and writes
shared/src/commonMain/composeResources/values{,-en}/strings.xml. Compose resources format only
positional arguments, so a lone %d / %s becomes %1$d / %1$s; Android's \\' escape becomes a plain
apostrophe. Comments and order are kept. Strings marked translatable="false" (the launcher label)
stay Android-only.
"""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PAIRS = [("values", "values"), ("values-en", "values-en")]


def convert(text: str) -> str:
    out = []
    for line in text.splitlines():
        if 'translatable="false"' in line:
            continue
        line = re.sub(r'<resources[^>]*>', '<resources>', line)
        line = line.replace("\\'", "'")
        line = re.sub(r'%([sd])', r'%1$\1', line)
        out.append(line)
    return "\n".join(out) + "\n"


def main() -> None:
    # Already run (web-app change): the app's strings now hold only Android-only texts, so a rerun
    # would wipe the shared texts.
    if (ROOT / "shared/src/commonMain/composeResources/values/strings.xml").exists():
        raise SystemExit("shared strings exist already; edit them there")
    for src, dst in PAIRS:
        source = ROOT / "app/src/main/res" / src / "strings.xml"
        target = ROOT / "shared/src/commonMain/composeResources" / dst / "strings.xml"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(convert(source.read_text(encoding="utf-8")), encoding="utf-8", newline="\n")
        print(f"{source.relative_to(ROOT)} -> {target.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
