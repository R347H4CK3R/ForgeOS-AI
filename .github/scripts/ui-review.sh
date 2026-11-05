#!/usr/bin/env bash
set -euo pipefail
cd android
gradle --no-daemon --max-workers=2 -PforgeAbi=x86_64 assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell run-as dev.forgeos.ai mkdir -p files
adb exec-in run-as dev.forgeos.ai tee files/test-model.gguf < ../test-model.gguf > /dev/null
result=0
gradle --no-daemon --max-workers=2 -PforgeAbi=x86_64 connectedDebugAndroidTest || result=$?
mkdir -p ../preview
adb pull /sdcard/Android/data/dev.forgeos.ai/files/review ../preview/ || true
adb exec-out run-as dev.forgeos.ai cat files/inference-result.txt > ../preview/inference-result.txt || true
adb logcat -d -t 2000 > ../preview/emulator-logcat.txt
exit "$result"
