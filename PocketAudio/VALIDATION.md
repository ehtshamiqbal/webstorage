# V7 validation

Source commit: 0942df6e2398c5e1c85815b1cd6efbf0c08af8cc. Successful workflow: 34766471063, job 103748223885.

Passed release compilation, lint, compact APK signing and ZIP alignment. Android 15 x86_64 emulator passed MP3, 1080p60 H.264/AAC, VP9/Opus and AV1/Opus conversion, title filenames, tabs, draft restoration, About, active engine version and session import/removal checks. Android MediaMetadataRetriever decoded visible frames from all three video fixtures. Android 10 x86_64 emulator passed fresh install, same-certificate replacement and launch.

Screenshots reviewed for readability and the revised glass/navigation design. All retained archive entries remain byte-identical after compression; only static link archives (.a) are removed. Final ARM APK payloads match the corresponding CI packages after permanent-key re-signing. Each contains only its intended ABI. The release certificate matches V5/V6: 5271ac53585666226567fdaf1cf326ace068b9cb12aa79945d14aab278f8810b.

ARM64: 54038373 bytes; SHA-256 5008683c26c40430faa5ef799d91d49efb451f3317b8236f5f0caee4fa4a915a.
ARM32: 47484785 bytes; SHA-256 d7f09c9c76ad1919415a8238c4963508e31ebe04f32ff65fbb4ea794a99c3396.

Tests use controlled HTTP media fixtures and dummy session cookies. No new live-platform, physical ARM device, every-phone, all-video-duration or 6–9 MB full-engine guarantee is made. Device tests use the CI certificate; final certificates and payload integrity are verified separately. Installed storage was not measured on the user's phone and is larger than APK size.
