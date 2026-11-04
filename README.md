# NEXUS SSH – Android Server Management Platform

Production-grade native Android application for Linux server management over SSH.

## Requirements
- Android Studio Ladybug or newer
- JDK 17 (Java 17)
- Android SDK API 35
- Min Android SDK API 26

## Building the APK
```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

Output APK is located in `app/build/outputs/apk/release/app-release-unsigned.apk`.
