# Nexus SSH design and engineering update

## Design system
- Warm ivory surfaces with a forest-green primary accent and a custom green wave wallpaper.
- Geist Sans is the primary UI family; Bricolage Grotesque is used for display and screen headings.
- Geist Mono is used for terminal output, paths, IP addresses, commands and other technical values.
- Phosphor Regular vectors are used for primary navigation, launcher glyphs and server identity options. The Docker whale remains a small custom brand-specific vector.
- Material Icons Extended remains as a compatibility dependency for screen-specific glyphs that have not yet been migrated.
- Font files are downloaded from the Google Fonts repository during Gradle `preBuild`, placed under generated Android resources, and bundled into the APK. They are not downloaded at runtime.

## Startup and lock behavior
- Branded animated Nexus loader is shown while saved server state is loading.
- App lock continues to use Android's system biometric prompt with device-credential fallback. No separate app PIN is stored.
- Existing secure-screen behavior and SSH credential encryption are unchanged.

## CI / quality gates
- GitHub Actions now runs `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug` for pushes, pull requests and manual runs.
- Unit tests cover Docker/service/port parsers, HTTPS port classification and terminal buffer behavior.
- Test results, lint report and build log are uploaded as a verification artifact even when a check fails.
- Version tags (`v*`) additionally build the release variant and publish the APK as a GitHub Release asset.

## Local verification performed for this handoff
- Pure Kotlin SSH parser and terminal-buffer smoke checks passed using the installed Kotlin compiler.
- XML/resource and project-structure checks are included in the handoff validation.
- Full Android Gradle build, Android lint and Gradle unit-test tasks could not be executed in this environment because Gradle / Android SDK tooling is not installed and external dependency/font downloads are unavailable here. GitHub Actions is configured to run those checks on the next push or PR.

## Known follow-up
- Material glyphs remain in some specialized screen components for compatibility; primary navigation and the core launcher/server icon set use Phosphor.
- Full UI/device screenshot regression testing still needs an Android emulator or physical device.
