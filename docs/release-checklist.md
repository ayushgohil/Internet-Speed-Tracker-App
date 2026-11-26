# Release readiness checklist

## Automated checks
- Run `./gradlew testDebugUnitTest assembleDebug assembleRelease` with JDK 17.
- Verify APK signatures and install/update behavior on a test device.
- Review lint findings using `./gradlew lintDebug`; resolve release-blocking issues.

## Device acceptance
- Test Android 8 and a recent Android version, including notification denial, permission revocation, Wi-Fi/mobile switching, reboot, overnight idle and explicit stop.
- Verify usage totals against Android settings; confirm no duplicate alerts and no cross-midnight spikes.
- Verify overlay positioning, lock-screen hiding, notification styles, quick settings, custom chart dates, backup/restore and malformed files.
- Exercise network tools on supported hardware; confirm tests are opt-in and discovery stops on leaving the screen.
- Measure battery impact and memory on real devices before making performance claims.

## Distribution
- Existing GitHub APKs are signed with a committed shared test key to preserve updates for testers. Treat them as test builds, not secure public distribution.
- Before public distribution, choose a protected signing key or Play App Signing, plan migration for existing installations, review current store target SDK and foreground-service/permission requirements, prepare a privacy policy and complete store declarations.
- Do not publish a store release using the shared test key. No store submission is part of this change.

## UI acceptance automation
The UI acceptance workflow runs Android 14 emulator smoke tests on pull requests and saves actual rendered screenshots, test reports, and diagnostic snapshots. Screenshots use a controlled UI state, not real subscriber usage. Emulator frame and memory snapshots are diagnostic only, not physical device battery or performance evidence.

## Physical battery profiling
Use the same optimized APK, device, network and screen brightness for each comparison. Record Android version, battery optimization configuration, signal conditions, refresh setting and foreground/background monitoring state. With the device unplugged, compare a monitoring-disabled baseline and monitoring-enabled run under similar idle conditions, ideally over several hours. Use Android battery usage and, where available, adb shell dumpsys batterystats (or Android Studio Power Profiler on supported devices). Do not reset personal battery statistics without consent. Save elapsed duration, battery percentage change, app CPU/wake activity, and any service interruptions. Repeat the runs before attributing a difference to NetPulse.

## Manual accessibility and visual acceptance
- Navigate all five destinations, nested tool and settings pages using TalkBack and Android back.
- Test system font sizes including 130% and 200%, display size, landscape and both themes.
- Verify labels, focus order, touch targets, contrast, and chart alternatives.
- Inspect emulator screenshots and replace README images only with reviewed, representative screenshots.
- Measure frame timing using a release build during scrolling and sustained traffic; do not infer smoothness from screenshot capture.
