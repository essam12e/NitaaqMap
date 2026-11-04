#!/usr/bin/env bash
# Drives the release APK on an emulator and saves Play Store screenshots to shots/.
set -x
PKG=com.nitaaq.map
ACT=$PKG/.MainActivity
LINK="https://www.google.com/maps/search/?api=1&query=24.7136,46.6753"
mkdir -p shots
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put global sysui_demo_allowed 1
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" > /dev/null; }
demo enter
demo clock -e hhmm 1041
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4
demo notifications -e visible false

shot() { sleep "${2:-2}"; adb exec-out screencap -p > "shots/$1.png"; echo "saved $1"; }
tap() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null
  adb pull /sdcard/ui.xml ui.xml > /dev/null
  xy=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
t = sys.argv[1]
for n in ET.parse("ui.xml").iter("node"):
    if t in (n.get("text"), n.get("content-desc")):
        a, b, c, d = map(int, re.findall(r"\d+", n.get("bounds")))
        print((a + c) // 2, (b + d) // 2); break
PY
)
  if [ -z "$xy" ]; then echo "not found: $1"; cp ui.xml "shots/ui-missing.xml"; return 1; fi
  adb shell input tap $xy
}

for mode in no yes; do
  name=$([ $mode = yes ] && echo dark || echo light)
  adb shell cmd uimode night $mode
  adb shell am force-stop $PKG
  adb shell am start -W -n $ACT
  shot "1-home-$name" 4
  adb shell am start -W -a android.intent.action.SEND -t text/plain \
    --es android.intent.extra.TEXT "'$LINK'" -n $ACT
  adb shell input keyevent 111 || true
  shot "2-link-$name" 3
  tap "إنشاء رمز QR" && shot "3-qr-$name" 6
  if [ $mode = no ]; then
    sleep 300
    shot "4-expired-$name" 1
  fi
  adb shell input keyevent 4
  sleep 2
  tap "الإعدادات" && shot "5-settings-$name" 3
done
ls -la shots
