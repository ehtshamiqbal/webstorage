# V6 validation

Application source commit: 89ec46567dff68839c204198d25a2e4d63eeb5f6. Build/test workflow: 34764134571; job 103741982070.

## Passed

- Release compilation, Android lint, APK signature and ZIP alignment checks.
- Android 15 emulator: MP3, H.264/AAC 1080p60, VP9/Opus and AV1/Opus converted to Gallery-safe H.264/AAC; title-derived filenames; separate tabs; draft/quality restoration; readable About.
- Android-native decoding of visible frames from all three saved video fixtures.
- Instrumentation verifies the active bundled engine version is 2026.08.19.
- Session checks: domain boundary, valid import, invalid import preserves the existing session, and removal. These use dummy cookies; authenticated platform access was not tested.
- Android 10 emulator: fresh release installation, same-certificate replacement and crash-free launch after each.
- Screenshots inspected on Android 10 and 15 for layout/readability.
- Final signature verified with the V5 permanent release certificate; all application payload bytes unchanged after re-signing. CI device tests use the CI certificate.
- ARM64 ELF load-segment alignment is at least 16 KB. No 16 KB physical-device test is claimed.

## Final artifact

159937510 bytes. SHA-256: d2b69b77d7c676d52889c2de15dff523d75f7ba70da81c87f41e37fafc32b29a.
Certificate SHA-256: 5271ac53585666226567fdaf1cf326ace068b9cb12aa79945d14aab278f8810b.

## Limits

The user's failing YouTube/Instagram URL was not supplied, so that exact failure was not reproduced or verified fixed. Media tests use controlled public-style local HTTP fixtures, not live social-platform accounts. Server-side login, permissions and anti-bot restrictions cannot be removed by this app. Cookie import does not guarantee platform acceptance. Android 9 and older are unsupported; installation and playback on every phone are not guaranteed. The UI is an Android interpretation with cached environmental refraction, not Apple's proprietary renderer.
