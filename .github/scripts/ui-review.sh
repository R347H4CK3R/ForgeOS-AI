#!/usr/bin/env bash
set -euxo pipefail
cd android
# Match the observed onn display and reduce emulator rendering overhead.
adb shell wm size 800x1280
adb shell wm density 160
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
gradle --no-daemon --max-workers=2 -PforgeAbi=x86_64 assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell run-as dev.forgeos.ai mkdir -p files
# ADB exec-in can keep stdin open: stop after the exact verified file size.
timeout 90s adb exec-in run-as dev.forgeos.ai sh -c '"head -c 105454432 > files/test-model.gguf"' < ../test-model.gguf
adb exec-out run-as dev.forgeos.ai sha256sum files/test-model.gguf | grep '^2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d '
result=0
gradle --no-daemon --max-workers=2 -PforgeAbi=x86_64 connectedDebugAndroidTest || result=$?
mkdir -p ../preview
adb pull /sdcard/Android/data/dev.forgeos.ai/files/review ../preview/ || true
adb exec-out run-as dev.forgeos.ai cat files/inference-result.txt > ../preview/inference-result.txt || true
adb logcat -d -t 2000 > ../preview/emulator-logcat.txt
exit "$result"
