# Pocket Media 6.0

Android audio/video downloader with original-title filenames, Gallery-safe H.264/AAC modes, separate Download / Recent / Settings tabs, an animated splash and a Liquid Glass-inspired Android interface. The material samples a cached, diffused background with slight magnification and edge lighting; this is not Apple's native material or a general live-content blur engine.

## Install and compatibility

Android 10 (API 29) or newer. Universal APK includes arm64-v8a, armeabi-v7a and x86_64. Version code 6. The distributed APK uses the same permanent signing certificate as V5, so V5 can be updated in place. V4 uses a different certificate and must be removed first. Do not disable platform security protections. Generic warnings for sideloaded APKs can remain; no universal device guarantee is made.

## Download support

The build bundles checksum-verified yt-dlp 2026.08.19 and installs it once on upgrade. Manual engine updates remain available in Settings. Generic cookie warnings are no longer automatically classified as login failures; authentication, unavailable content, rate limits and player changes have separate explanations. Server-side authentication, entitlement, geographic restrictions and anti-bot checks are still controlled by the platform.

For a login-required video your account can access, Settings > Platform connection optionally imports a Netscape-format cookies.txt exported from your own browser. Treat the file like a password and never share it. Only supported platform domains and non-expired cookies are retained. Sessions stay in app-private, non-backed-up storage and can be removed in Settings. This is not an embedded sign-in flow or a way to unlock restricted content. Public downloads do not require an app account.

## Quality and speed

720p/1080p Gallery-safe modes prefer H.264/AAC and transcode incompatible tracks as needed. 1080p60 has a separate choice. Best / 4K / 6K / 8K Original preserve source codecs and depend on player/device capabilities. No upscaling or instant-download claim is made. Initialization, extractor caching and concurrent fragments remain; unchanged UI state is no longer repeatedly rendered.

## Build

JDK 17, Gradle 8.11.1, Android SDK 35, NDK 28.0.13004108, CMake 3.22.1.

```sh
cd PocketAudio
python3 scripts/prepare_lame.py
python3 scripts/prepare_engine.py
gradle --no-daemon assembleRelease assembleReleaseAndroidTest lintRelease
```

CI signs a non-debuggable release with a temporary certificate for instrumentation. Distribution requires re-signing with the retained permanent key. Never commit private keys, passwords or browser sessions. See VALIDATION.md. License: GPL-3.0 and applicable upstream notices.
