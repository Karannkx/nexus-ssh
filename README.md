<div align="center">

# 🔐 Nexus SSH

### A modern, secure, native Android SSH client — terminal, SFTP, Docker & systemd in your pocket.

![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Build](https://img.shields.io/github/actions/workflow/status/Karannkx/nexus-ssh/build-apk.yml?style=for-the-badge&logo=githubactions&logoColor=white&label=Build)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen?style=for-the-badge)

</div>

---

## ✨ What is Nexus SSH?

**Nexus SSH** is a fully native Android app (100% Kotlin + Jetpack Compose) that lets you manage Linux servers straight from your phone. Connect over SSH, run commands in a real PTY shell, browse and edit remote files over SFTP, watch live server metrics, and control Docker containers and systemd services — all with credentials encrypted by the hardware-backed Android Keystore.

> 💡 No demo data, no mock screens: everything you see in the app comes from your real server.

---

## 🚀 Features

| | Feature | Details |
|---|---|---|
| 🖥️ | **Multi-server profiles** | Save unlimited servers in a local Room database, switch between them from the top bar |
| 🔑 | **Password & private-key auth** | RSA / ED25519 / ECDSA keys (OpenSSH or PEM), optional passphrase, import key from a file |
| 🛡️ | **Hardware-backed encryption** | Passwords, keys and passphrases are encrypted with AES-256-GCM via Android Keystore and never stored in plaintext |
| 🔍 | **Host-key verification (TOFU)** | Shows the real `SHA256:` fingerprint on first connect, remembers it, and **refuses** to connect if it later changes |
| ⌨️ | **Interactive terminal** | Real PTY shell (`xterm-256color`), auto-sized to your screen, with ESC / TAB / Ctrl-C / Ctrl-D / Ctrl-Z / arrow keys |
| 📁 | **SFTP file manager** | Browse folders, edit & save text files (up to 1 MB), create folders, delete files/folders |
| 📊 | **Live dashboard** | CPU %, memory, disk, load average, uptime, OS, kernel & hostname — refreshed every 5 s |
| 🐳 | **Docker manager** | List all containers, start / stop / restart (auto-falls back to `sudo -n` when needed) |
| ⚙️ | **systemd services** | Search & filter units, start / stop / restart with a confirmation dialog |
| 🔌 | **Connection watchdog** | SSH keep-alive plus automatic detection of dropped connections |

---

## 🧰 Tech Stack

| Layer | Technology |
|---|---|
| 🧑‍💻 Language | [Kotlin](https://kotlinlang.org/) 2.0.21 |
| 🎨 UI | [Jetpack Compose](https://developer.android.com/jetpack/compose) + Material 3 (Compose BOM `2024.12.01`) |
| 🏗️ Architecture | Single-activity, `ViewModel` + `StateFlow` (unidirectional data flow) |
| ⚡ Async | Kotlin Coroutines & Flow `1.9.0` |
| 🗄️ Storage | [Room](https://developer.android.com/training/data-storage/room) `2.6.1` with [KSP](https://github.com/google/ksp) |
| 🔐 Crypto | Android Keystore (AES-256-GCM) · [BouncyCastle](https://www.bouncycastle.org/) `1.78.1` |
| 📡 SSH / SFTP | [SSHJ](https://github.com/hierynomus/sshj) `0.38.0` |
| 🔨 Build | Android Gradle Plugin `8.7.2` · Gradle `8.10.2` · JDK 17 |
| 🤖 CI | GitHub Actions (builds a debug APK on every push / PR) |

**SDK levels:** `minSdk 26` (Android 8.0) · `targetSdk 35` · `compileSdk 35`

---

## 📸 Screens

| Tab | What you get |
|---|---|
| 📊 **Dashboard** | Connection status + live CPU / RAM / disk meters and system info |
| ⌨️ **Terminal** | Full interactive shell with a special-keys bar |
| 📁 **Files** | SFTP browser with built-in text editor |
| 🐳 **Docker** | Container list with one-tap actions |
| ⚙️ **Services** | Searchable systemd unit list with actions |

*(Add screenshots to a `docs/` folder and link them here once you have them.)*

---

## 🛠️ Getting Started

### ✅ Prerequisites

- 🧑‍💻 **Android Studio** Ladybug (2024.2) or newer
- ☕ **JDK 17** (bundled with recent Android Studio)
- 📱 An Android device or emulator running **Android 8.0+**
- 🖧 A Linux server you can reach over SSH to test with

### 📥 1. Clone

```bash
git clone https://github.com/Karannkx/nexus-ssh.git
cd nexus-ssh
```

### ▶️ 2. Run from Android Studio

1. Open the project folder in Android Studio.
2. Let Gradle sync finish.
3. Pick a device/emulator and press **Run ▶**.

### 🏗️ 3. Build from the command line

> ℹ️ The Gradle wrapper isn't committed yet. Install [Gradle 8.10.2](https://gradle.org/releases/) (or run `gradle wrapper --gradle-version 8.10.2` once to generate `gradlew`).

```bash
# Debug APK
gradle :app:assembleDebug

# Output:
# app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> 📦 The debug build uses the package id `com.nexus.ssh.debug`, so it can live next to a release build.

### 🤖 4. Get an APK from CI (no setup needed)

Every push and pull request runs the **Build Android APK** workflow.
Open the **Actions** tab → latest run → download the **`nexus-ssh-apk`** artifact.

---

## 📖 How to use

1. ➕ Tap **+** and add a server (host, port, username, password *or* private key).
2. 🔌 Tap **Connect**.
3. 🔍 On first connection, compare the displayed **fingerprint** with your server's and tap **Trust & connect**.
4. 🎉 Use the bottom tabs to monitor, run commands, edit files, and manage Docker / services.

> 🔎 Tip — verify a server fingerprint with:
> `ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub`

---

## 🗂️ Project Structure

```text
nexus-ssh/
├── .github/workflows/build-apk.yml      # CI: builds the debug APK
├── app/
│   └── src/main/java/com/nexus/ssh/
│       ├── MainActivity.kt              # Entry point, scaffold & navigation
│       ├── NexusViewModel.kt            # App state + all server actions
│       ├── NexusApplication.kt          # BouncyCastle provider setup
│       ├── core/
│       │   ├── database/                # Room: ServerEntity, ServerDao, NexusDatabase
│       │   ├── security/                # KeystoreManager (AES-256-GCM)
│       │   ├── ssh/                     # SSH connection, PTY session, terminal buffer, parsers
│       │   └── sftp/                    # SftpManager
│       └── ui/
│           ├── Theme.kt                 # Colors & Material 3 theme
│           └── screens/                 # Dashboard, Terminal, Files, Docker/Services, dialogs
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🔒 Security

- 🔐 Secrets are encrypted with a non-exportable **AES-256-GCM** key stored in the Android Keystore.
- 🚫 Backups are disabled; the database and preferences are excluded from cloud backup and device transfer.
- 🧬 Host keys are pinned with **trust-on-first-use**; a changed key is a hard failure, not a prompt.
- 🧹 Remote commands built from user-visible data (container IDs, unit names) are validated and quoted.

Found a vulnerability? Please **don't** open a public issue — contact the maintainer privately via the email on the GitHub profile.

---

## 🗺️ Roadmap

- [ ] 📤 SFTP upload / download
- [ ] 🔀 SSH port forwarding / tunnels
- [ ] 👆 Biometric app lock
- [ ] ✏️ Edit existing server profiles
- [ ] 🗝️ Generate SSH keys in-app
- [ ] 🖼️ Full-screen terminal emulation (vim, htop, tmux)
- [ ] 📜 Docker logs & systemd journal viewer
- [ ] 🧪 Unit & UI tests

Have an idea? [Open an issue](https://github.com/Karannkx/nexus-ssh/issues) 💬

---

## 🤝 Contributing

Contributions are **very welcome** — from typo fixes to whole features! 🎉

1. 🍴 **Fork** the repository
2. 🌿 **Create a branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. 💻 **Make your changes** and make sure it builds:
   ```bash
   gradle :app:assembleDebug
   ```
4. ✅ **Commit** with a clear message
   ```bash
   git commit -m "feat: add amazing feature"
   ```
5. 🚀 **Push** and open a **Pull Request** against `main`
   ```bash
   git push origin feature/amazing-feature
   ```

### 📝 Guidelines

- Follow the official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Keep UI in Compose and logic in the `ViewModel` / `core` packages.
- Never commit secrets, keys, or real server addresses.
- Use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:` …) where possible.
- For big changes, open an issue first so we can discuss the approach.

### 🐛 Reporting bugs

Please include your Android version, device model, what you expected vs. what happened, and (without secrets!) any relevant logcat output.

---

## ⚠️ Limitations

- The terminal is line-oriented; full-screen TUI apps are not rendered correctly yet.
- SFTP editing is limited to text files up to 1 MB.
- Docker / systemd actions require your user to have the needed permissions (or passwordless `sudo`).

---

## 📄 License

Released under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with ❤️ by [Karannkx](https://github.com/Karannkx)

⭐ **If you find Nexus SSH useful, please star the repo!** ⭐

</div>
