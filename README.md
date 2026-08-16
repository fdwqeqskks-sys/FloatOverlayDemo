# Float Overlay Demo

Android 10 (API 29) Java project with a foreground overlay service and a JNI
Float reader for addresses mapped inside the app's own process.

## Toolchain

- Android SDK Platform 29
- Android SDK Build-Tools 29.0.3
- Android NDK with CMake 3.10.2
- JDK 8 or JDK 11
- Gradle 6.7.1
- Android Gradle Plugin 4.2.2

## Run

1. Install Android SDK Platform 29, Build-Tools 29.0.3, NDK, and CMake 3.10.2.
2. Open this directory in Android Studio, select Gradle 6.7.1, and sync.
3. Run on an Android 6.0-10 device or emulator.
4. Tap `Create demo address`, then `Read once` to verify native reads.
5. Tap `Start overlay` and grant the system overlay permission.

The demo address points to a native static Float in this application process.
The native reader validates the full Float range against `/proc/self/maps` before
copying the value. Addresses from another process are outside this demo's scope.
