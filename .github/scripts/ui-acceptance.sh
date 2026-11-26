#!/usr/bin/env bash
set -uo pipefail
mkdir -p ui-artifacts
./gradlew connectedDebugAndroidTest --stacktrace
result=$?
for name in dark-overview dark-tools dark-settings light-large-apps; do
  adb exec-out run-as com.ayush.netpulse cat "files/ui-screenshots/$name.png" > "ui-artifacts/$name.png" || rm -f "ui-artifacts/$name.png"
done
adb shell dumpsys gfxinfo com.ayush.netpulse > ui-artifacts/gfxinfo.txt || true
adb shell dumpsys meminfo com.ayush.netpulse > ui-artifacts/meminfo.txt || true
exit "$result"
