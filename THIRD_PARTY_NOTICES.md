# Third-party assets and libraries

Nexus SSH includes or downloads the following open-source design assets:

| Asset | Use | Source | License |
|---|---|---|---|
| Geist Sans | Main UI typography | https://github.com/vercel/geist-font | SIL Open Font License 1.1 |
| Geist Mono | Terminal and technical text | https://github.com/vercel/geist-font | SIL Open Font License 1.1 |
| Bricolage Grotesque | Display and screen headings | https://github.com/ateliertriay/bricolage | SIL Open Font License 1.1 |
| Phosphor Icons for Compose | Server identity and selected interface icons | https://github.com/adamglin0/compose-phosphor-icon | MIT |

The font files are downloaded from the Google Fonts source repository at pinned commit `718e1db4deb9e4d9d85a0ead1b9f5fde2761ccfd` during the Android Gradle `preBuild` phase and are bundled into the APK. They are not fetched at runtime. A network connection is required for the first build on a clean checkout; subsequent builds reuse the generated files in the Gradle build directory.

Phosphor icon library coordinates: `com.adamglin:phosphor-icon:1.0.0`.

The existing Material icon dependency is retained temporarily because some specialized screens still use Material glyphs. The main server identity icon set now uses Phosphor vectors; the remaining screen-by-screen migration can be done without breaking the current feature set.
