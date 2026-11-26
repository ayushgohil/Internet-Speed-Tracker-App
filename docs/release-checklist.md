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
