# Project-specific ProGuard / R8 rules.
# Room, WorkManager, Compose and the AndroidX libraries ship their own consumer rules, and classes
# declared in AndroidManifest.xml (service, receivers, activity) are kept automatically.

# Keep line numbers so crash stack traces from release builds are readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
