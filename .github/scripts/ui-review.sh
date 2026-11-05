#!/usr/bin/env bash
set -euxo pipefail
cd android
APP_ID=dev.forgeos.ai.review
# Match the observed onn display and reduce emulator rendering overhead.
adb shell wm size 800x1280
adb shell wm density 160
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
gradle --no-daemon --max-workers=2 -PforgeAbi=x86_64 assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Use ADB's file-sync protocol, not raw stdin, for binary model transfer.
MODEL=/sdcard/Android/data/${APP_ID}/files/test-model.gguf
adb shell mkdir -p /sdcard/Android/data/${APP_ID}/files
timeout 90s adb push ../test-model.gguf "$MODEL"
adb shell wc -c "$MODEL"
adb shell sha256sum "$MODEL" | tee ../emulator-model.sha256
grep '^2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d ' ../emulator-model.sha256
result=0
gradle --no-daemon --max-workers=2 -PforgeAbi=x86_64 connectedDebugAndroidTest || result=$?
mkdir -p ../preview
adb pull /sdcard/Android/data/${APP_ID}/files/review ../preview/ || true
adb pull /sdcard/Android/data/${APP_ID}/files/inference-result.txt ../preview/inference-result.txt || true
adb logcat -d -t 2000 > ../preview/emulator-logcat.txt
exit "$result"
