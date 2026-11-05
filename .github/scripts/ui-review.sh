#!/usr/bin/env bash
set -euxo pipefail
cd android
APP_ID=dev.forgeos.ai.review
adb shell wm size 800x1280
adb shell wm density 160
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
gradle --no-daemon --max-workers=2 -PforgeAbi=x86_64 assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
MODEL=/sdcard/Android/data/${APP_ID}/files/test-model.gguf
adb shell mkdir -p /sdcard/Android/data/${APP_ID}/files
timeout 90s adb push ../test-model.gguf "$MODEL"
adb shell wc -c "$MODEL"
adb shell sha256sum "$MODEL" | tee ../emulator-model.sha256
grep '^2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d ' ../emulator-model.sha256
mkdir -p ../preview
# Run the same AndroidJUnitRunner directly so AGP cannot uninstall the app and
# delete screenshots/model output before they are collected.
result=0
timeout 240s adb shell am instrument -w -r -e class dev.forgeos.ai.NativeReviewTest "${APP_ID}.test/androidx.test.runner.AndroidJUnitRunner" | tee ../preview/instrumentation.txt || result=$?
adb pull /sdcard/Android/data/${APP_ID}/files/review ../preview/ || result=1
adb pull /sdcard/Android/data/${APP_ID}/files/inference-result.txt ../preview/inference-result.txt || result=1
adb logcat -d -t 2000 > ../preview/emulator-logcat.txt
# am instrument itself can return zero on failed tests: assert the runner result.
grep -Eq '^OK \(5 tests\)' ../preview/instrumentation.txt || result=1
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' ../preview/instrumentation.txt; then result=1; fi
test -s ../preview/review/home-portrait.png || result=1
test -s ../preview/review/landscape.png || result=1
exit "$result"
