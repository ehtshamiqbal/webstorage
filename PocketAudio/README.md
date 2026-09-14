# Pocket Media 7.0 — compact Android packages

V7 retains V6's on-device MP3/MP4 download and conversion engine, original-title filenames, Gallery-safe quality modes, platform-session import and separate tabs.

## Install

Android 10 or newer. Choose the APK matching the phone's Android ABI: ARM64 for a 64-bit ARM system, ARM32 for a 32-bit ARM system. Both are complete installable APKs; they are alternatives, not files to install together. Processor hardware alone does not establish whether the installed Android OS is 32- or 64-bit. The same permanent certificate as V5/V6 allows an in-place update. V4 used a different certificate.

## Size

V6 universal APK: 159937510 bytes. V7 ARM64: 54038373 bytes (51.54 MiB); ARM32: 47484785 bytes (45.29 MiB). About 66% / 70% smaller downloads, respectively.

Installed storage is higher than APK size because native packages unpack, Android optimizes code, and caches accumulate. These are APK measurements, not a claim that installed storage is 45–52 MiB.

Changes: ABI-specific APKs; lossless ZIP compression; remove unused static .a archives; remove unused JNI MP3 bridge (current conversion uses FFmpeg's encoder); replace duplicate engine assets with one raw resource. Every retained runtime archive entry is checked byte-for-byte during packaging. No runtime codec, resolution option or server-side conversion dependency was introduced or removed.

## Interface

Stronger edge refraction using a cached bitmap mesh, translucent layers, press highlights and drawn navigation icons. This is an Android interpretation inspired by Liquid Glass, not Apple's native renderer or full live-content backdrop blur.

## Build

JDK 17, Gradle 8.11.1 and Android SDK 35.

```sh
cd PocketAudio
python3 scripts/prepare_lame.py
python3 scripts/prepare_engine.py
gradle --no-daemon assembleRelease assembleReleaseAndroidTest lintRelease :app:writeTestSigningPath
python3 scripts/compact_apks.py
```

The LAME preparation step supplies license assets; its old JNI bridge is no longer built. Packaging uses Gradle's CI test key. Distribution APKs must be signed with the privately retained permanent key; do not publish signing keys, passwords or browser sessions.

Platform authentication, entitlement and anti-bot checks remain controlled by the platform. Optional browser sessions stay in private non-backed-up storage. See VALIDATION.md for test scope. GPL-3.0 and applicable upstream licenses.
