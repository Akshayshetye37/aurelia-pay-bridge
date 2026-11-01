#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE="$ROOT/assets/aurelia-icon.jpg"
RES="$ROOT/app/src/main/res"
test -s "$SOURCE" || { echo "::error::Approved Aurelia icon asset is missing"; exit 1; }
for density in mdpi hdpi xhdpi xxhdpi xxxhdpi nodpi; do
  dir="$RES/mipmap-$density"
  mkdir -p "$dir"
  rm -f "$dir"/ic_launcher.png "$dir"/ic_launcher.webp "$dir"/ic_launcher.jpg \
        "$dir"/ic_launcher_round.png "$dir"/ic_launcher_round.webp "$dir"/ic_launcher_round.jpg \
        "$dir"/ic_launcher_foreground.png "$dir"/ic_launcher_foreground.webp "$dir"/ic_launcher_foreground.jpg
  cp "$SOURCE" "$dir/ic_launcher.jpg"
  cp "$SOURCE" "$dir/ic_launcher_round.jpg"
  if [[ "$density" == "nodpi" ]]; then cp "$SOURCE" "$dir/ic_launcher_foreground.jpg"; fi
done
mkdir -p "$RES/mipmap-anydpi-v26" "$RES/values"
cat > "$RES/mipmap-anydpi-v26/ic_launcher.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/aurelia_icon_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
XML
cp "$RES/mipmap-anydpi-v26/ic_launcher.xml" "$RES/mipmap-anydpi-v26/ic_launcher_round.xml"
cat > "$RES/values/aurelia_icon_colors.xml" <<'XML'
<?xml version="1.0" encoding="utf-8"?>
<resources><color name="aurelia_icon_background">#082419</color></resources>
XML
python3 - "$ROOT/app/src/main/AndroidManifest.xml" <<'PY'
import sys
from pathlib import Path
p = Path(sys.argv[1])
s = p.read_text()
start = s.find("<application")
end = s.find(">", start)
if start < 0 or end < 0:
    raise SystemExit("AndroidManifest.xml has no application element")
tag = s[start:end]
import re
tag = re.sub(r'\s+android:icon="[^"]*"', '', tag)
tag = re.sub(r'\s+android:roundIcon="[^"]*"', '', tag)
tag += ' android:icon="@mipmap/ic_launcher" android:roundIcon="@mipmap/ic_launcher_round"'
p.write_text(s[:start] + tag + s[end:])
PY
echo "Installed Aurelia PMS branded launcher icon for Pay Bridge."
