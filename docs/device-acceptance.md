# NetPulse v1.2 device acceptance

Run on a physical Android device with the same APK build for each comparison. Record device model, Android version, refresh rate, build SHA, network type and ambient conditions.

## Accessibility
- Enable TalkBack and navigate every tab, settings category and tool. Verify focus order, names, switch state and back navigation.
- Repeat with largest font and display size; verify no clipped controls, overlapping counters or inaccessible actions.
- Disable animations in Android accessibility settings and confirm navigation and counters remain usable.
- Check light and dark themes, contrast and 48 dp interactive targets.

## Frame performance
With USB debugging enabled and adb available, reset frame stats before each run:
```sh
adb shell dumpsys gfxinfo com.ayush.netpulse reset
```
Observe live graph during a sustained download for 60 seconds, then switch tabs repeatedly. Capture:
```sh
adb shell dumpsys gfxinfo com.ayush.netpulse framestats > frames.txt
```
Compare janky frames and frame duration with the previous build under matching conditions.

## Battery
Compare monitoring enabled and disabled in separate matched 30-minute sessions, with screen off and comparable traffic, brightness and radio conditions. Do not reset device-wide battery statistics. Capture before and after each session:
```sh
adb shell dumpsys batterystats com.ayush.netpulse > battery-before.txt
adb shell dumpsys batterystats com.ayush.netpulse > battery-after.txt
```
Record wake locks, CPU time and estimated consumption when available. Repeat to reduce noise. These estimates are not direct power measurements.

## Screenshots
Open each screen manually and capture using:
```sh
adb exec-out screencap -p > overview.png
```
Repeat for Usage, Apps, Tools, Settings and both themes. Use test data or redact private app names, SSIDs and addresses before publishing. Do not replace screenshots with mockups.

## Release gate
Build and unit tests must pass. Record results and regressions before tagging a stable release. No device results are claimed by this document.
