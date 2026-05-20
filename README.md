# CloudNG

<p align="center">
  <img src="https://img.shields.io/badge/Android-24%2B-brightgreen" alt="Android 7.0+">
  <img src="https://img.shields.io/badge/Kotlin-2.1.21-blue" alt="Kotlin 2.1.21">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material3-purple" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Xray--core-v26.5.9-orange" alt="Xray-core">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License: MIT">
</p>

A modern, open-source Android VPN/proxy client built with **Jetpack Compose** and powered by the **Xray-core** library. CloudNG provides a clean, intuitive interface for managing proxy connections with support for multiple protocols.

> **Note:** This is a clean-room implementation inspired by v2rayNG, built with modern Android architecture and Material Design 3.

---

## Features

### Core Capabilities
- **Xray-core Integration** — Real Xray-core library (v26.5.9) with native `.so` libraries
- **VPN Service** — Full system-level VPN tunneling via Android's `VpnService` API
- **Traffic Statistics** — Real-time upload/download stats with latency measurement
- **Auto-Start on Boot** — Optional VPN connection on device startup

### Supported Protocols
| Protocol | Status |
|----------|--------|
| VMESS | ✅ Full Support |
| VLESS | ✅ Full Support |
| Trojan | ✅ Full Support |
| Shadowsocks | ✅ Full Support |
| SOCKS5 | ✅ Full Support |
| HTTP | ✅ Full Support |
| WireGuard | ✅ Full Support |

### Modern Android Architecture
- **Jetpack Compose** — Declarative UI with Material Design 3
- **MVVM + Repository Pattern** — Clean separation of concerns
- **Hilt Dependency Injection** — Type-safe DI
- **Room Database** — Local data persistence
- **WorkManager** — Background subscription updates
- **DataStore** — Type-safe preferences

### User Experience
- **Material Design 3** — Modern, responsive UI with dynamic color support
- **QR Code Import** — Scan proxy configurations from QR codes
- **Subscription Management** — Auto-update proxy lists from subscription URLs
- **Per-App Proxy** — Selective application routing
- **Custom Routing Rules** — Flexible traffic routing configuration
- **Dark/Light Themes** — Automatic system theme detection

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| UI Framework | Jetpack Compose (Material 3) |
| Language | Kotlin 2.1.21 |
| Architecture | MVVM + Repository Pattern |
| Dependency Injection | Hilt |
| Database | Room (SQLite) |
| Networking | HttpURLConnection, Gson |
| Background Work | WorkManager |
| Preferences | DataStore |
| QR Scanning | ZXing Android Embedded |
| VPN Core | libv2ray (Xray-core v26.5.9) |

---

## Requirements

- **Android 7.0+** (API 24+)
- **Target SDK:** 36 (Android 16)
- **Compile SDK:** 36

---

## Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17 or higher
- Android SDK with API 36

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/yourusername/CloudNG.git
cd CloudNG

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease
```

### Signing Configuration (Release Builds)

For release builds, configure signing in your **local** `local.properties` file (do not commit this file):

```properties
RELEASE_STORE_FILE=../cloudng-release.jks
RELEASE_STORE_PASSWORD=your_secure_password
RELEASE_KEY_ALIAS=cloudng
RELEASE_KEY_PASSWORD=your_secure_password
```

Or use environment variables:
```bash
export RELEASE_STORE_FILE=../cloudng-release.jks
export RELEASE_STORE_PASSWORD=your_secure_password
export RELEASE_KEY_ALIAS=cloudng
export RELEASE_KEY_PASSWORD=your_secure_password
```

---

## Project Structure

```
app/src/main/java/com/cloudng/app/
├── core/                    # Core bridge interfaces and implementations
│   ├── CoreBridge.kt        # Abstract core interface
│   ├── XrayCoreBridge.kt    # Real Xray-core implementation
│   ├── StubCoreBridge.kt     # Stub for testing
│   └── ProfileParser.kt      # Protocol parsers
├── data/
│   ├── db/                   # Room database, DAOs
│   ├── model/                # Data models (Profile, Subscription, etc.)
│   └── repository/           # Repository implementations
├── di/                       # Hilt modules
├── receiver/                 # Broadcast receivers (Boot, VPN control)
├── service/                  # VPN and proxy services
│   ├── CloudVpnService.kt    # Main VPN service
│   └── CloudProxyService.kt  # Proxy helper service
├── ui/                       # Compose UI screens
│   ├── home/                 # Main screen with VPN toggle
│   ├── profiles/             # Profile management
│   ├── subscriptions/        # Subscription management
│   ├── routing/              # Routing rules & per-app proxy
│   ├── dns/                  # DNS configuration
│   ├── logs/                 # Log viewer
│   ├── settings/             # App settings
│   ├── onboarding/           # First-launch onboarding
│   ├── navigation/           # Navigation graph
│   ├── components/           # Reusable UI components
│   └── theme/                # Material 3 theme
└── work/                     # WorkManager workers

app/src/main/
├── assets/                   # geoip.dat, geosite.dat
├── libs/                     # libv2ray.aar
└── res/                      # Android resources
```

---

## Screenshots

*Screenshots will be added soon*

---

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details on:
- Code style and conventions
- Pull request process
- Development setup

### Development Guidelines
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use [Conventional Commits](https://www.conventionalcommits.org/)
- Write tests for new features
- Update documentation as needed

---

## License

```
MIT License

Copyright (c) 2025 CloudNG Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Acknowledgments

- **[Xray-core](https://github.com/XTLS/Xray-core)** — The powerful proxy platform
- **[AndroidLibXrayLite](https://github.com/2dust/AndroidLibXrayLite)** — Android wrapper for Xray-core
- **[v2rayNG](https://github.com/2dust/v2rayNG)** — Inspiration for this project
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)** — Modern Android UI toolkit

---

## Disclaimer

This application is intended for **legitimate network research and privacy protection** purposes only. Users are responsible for complying with local laws and regulations. The developers assume no liability for misuse of this software.

---

<p align="center">
  Made with ❤️ using Kotlin & Jetpack Compose
</p>
