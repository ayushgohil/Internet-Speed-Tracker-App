#!/usr/bin/env bash
# Runs inside the emulator: installs the APKs the way a phone would and prints what happened.
# Never stops on errors, so every result ends up in the log.
D=app/build/outputs/apk/debug/app-debug.apk
R=app/build/outputs/apk/release/app-release.apk
P=com.ayush.netpulse

section() { echo; echo "################ $*"; }

section "Device"
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk

section "1) Fresh install of DEBUG apk"
adb install "$D"; echo "exit code: $?"

section "2) Package facts"
adb shell dumpsys package $P | grep -E "versionCode|versionName|minSdk|targetSdk|flags=|firstInstallTime" | head -8

section "3) Update: install RELEASE over DEBUG (same key, same versionCode)"
adb install -r "$R"; echo "exit code: $?"

section "4) Launch the app and check it stays alive"
adb shell am start -n $P/.ui.MainActivity
sleep 10
echo "pid: $(adb shell pidof $P)"
adb logcat -d | grep -E "AndroidRuntime|FATAL EXCEPTION|SpeedTrackerService|ServiceRevival" | tail -25

section "5) Uninstall, then fresh install of RELEASE apk"
adb uninstall $P
adb install "$R"; echo "exit code: $?"
adb shell dumpsys package $P | grep -E "versionCode|flags=" | head -3

section "6) Install DEBUG over RELEASE (the other direction)"
adb install -r "$D"; echo "exit code: $?"

section "DONE"
