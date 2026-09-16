<div align="center">
<img src="media/app_icon.png" width="160" height="160" style="display: block; margin: 0 auto"/>
<h1>Network Switch</h1>
<p>Modern Android app for network mode switching</p>

[![Build Status](https://img.shields.io/github/actions/workflow/status/markvoronin354/NetworkSwitch/build.yml)](https://github.com/markvoronin354/NetworkSwitch/actions)
[![License](https://img.shields.io/github/license/markvoronin354/NetworkSwitch)](https://github.com/markvoronin354/NetworkSwitch/blob/main/LICENSE)
[![Downloads](https://img.shields.io/github/downloads/markvoronin354/NetworkSwitch/total)](https://github.com/markvoronin354/NetworkSwitch/releases)
[![Release](https://img.shields.io/github/v/release/markvoronin354/NetworkSwitch)](https://github.com/markvoronin354/NetworkSwitch/releases/latest)
[![Awesome](https://awesome.re/mentioned-badge-flat.svg)](https://github.com/timschneeb/awesome-shizuku)

</div>

---

A modern, open-source Android application built with **Jetpack Compose**, **Clean Architecture**, and **Material Design 3** that enables users to toggle between cellular network modes. Supports 34 different network configurations (2G, 3G, 4G, and 5G combinations) using dual system control methods: **Root access** (libsu) or **Shizuku** service for non-rooted devices.

## Purpose

Network Switch provides granular control over device cellular network radio modes, supporting 34 different configurations ranging from pure single-mode forced configurations (e.g. 5G SA/NR-only, 4G LTE-only) to complex multi-mode preference stacks.

### Key Use Cases
- **Quick Switching**: Instantly toggle between configured network modes directly from the Quick Settings panel.
- **Speed & Signal Optimization**: Force high-performance modes (e.g., 5G/NR or 4G/LTE) in areas with poor auto-selection logic.
- **Battery Preservation**: Drop down to power-efficient network modes when high-speed data is not needed.
- **Coverage & Roaming**: Force specific radio bands when traveling internationally or using specialized carrier configurations.
- **Privacy First**: No internet permissions required—all operations run locally on device.

## Features

- **34 Network Modes**: Comprehensive coverage for global carriers, CDMA (US), TD-SCDMA (China), and standard GSM/WCDMA/LTE/NR stacks.
- **Smart Quick Settings Tile**: Dynamically renders active network technology text (**5G**, **4G**, **3G**, **2G**) directly inside the tile icon, rendering clearly on both compact circular QS panels and full expanded tiles.
- **Configurable Toggle Pairs**: Select any two network modes (Mode A / Mode B) for instant 1-tap toggling.
- **Dual System Operations**:
  - **Shizuku Method**: Non-rooted system access via Shizuku API.
  - **Root Method**: Direct root access via `libsu` service.
- **Modern Clean Architecture**: Decoupled Data, Domain, and Presentation layers powered by Hilt dependency injection, DataStore preferences, and Kotlin Flow.
- **Material 3 UI**: Clean, reactive Compose interfaces with dynamic theming.

## Configuration

### Supported Network Modes

- **Basic Modes**: GSM Only (2G), WCDMA Only (3G), LTE Only (4G), NR Only (5G).
- **Combined Modes**: 2G/3G preference stacks, 3G/4G (LTE/WCDMA), 4G/5G (NR/LTE), and 2G/3G/4G/5G combined.
- **Regional & Global Modes**: CDMA / EvDo (US carriers), TD-SCDMA (China), and Global 4G/5G international stacks.

### Setting Up Toggle Configuration
1. Open the app and navigate to **Network Mode Config** / settings.
2. Select **Mode A** and **Mode B** from the available network mode list.
3. Save configuration.
4. Tap the Quick Settings tile or main app toggle to switch between the two modes instantly.

## Screenshots

<div align="center">
<img src="media/screenshot_main.jpg" alt="Main Screen" width="240" />
<img src="media/screenshot_settings.jpg" alt="Settings Screen" width="240"/>
</div>

## Requirements

- **Android Version**: Android 10 (API level 29) or higher.
- **Control Method Requirement** (Choose one):
  - **Shizuku Method**: Shizuku app installed and running (via Wireless ADB or ADB connection).
  - **Root Method**: Rooted Android device with root permissions granted to Network Switch.

## Installation
<div align="center">

[<img src="media/get_it_github.png" alt="Get it on GitHub" height="80" align="center">](https://github.com/markvoronin354/NetworkSwitch/releases)
[<img src="https://img.shields.io/badge/Get_it_on-Orion_Store-1A73E8?style=for-the-badge&logo=android&logoColor=white" alt="Get it on Orion Store" height="80" align="center">](https://rookieenough.github.io/Orion-Data/redirect.html?id=network-switch)

</div>

1. Download the latest APK from the Releases tab.
2. Install the APK on your device.
3. Grant access permissions for your preferred control method (**Shizuku** or **Root**).
4. Add the **Network Switch** tile to your Quick Settings panel.

## Architecture & Project Structure

The project follows modern Android Clean Architecture and MVVM patterns:

```
app/
├── src/main/
│   ├── java/com/supernova/networkswitch/
│   │   ├── presentation/               # UI Layer (MVVM & Compose)
│   │   │   ├── ui/
│   │   │   │   ├── activity/          # MainActivity, SettingsActivity, NetworkModeConfigActivity
│   │   │   │   ├── composable/        # Reusable Compose views & selectors
│   │   │   │   └── components/        # Shared Material 3 components
│   │   │   ├── viewmodel/             # ViewModels handling UI state
│   │   │   └── theme/                 # Material Design 3 theme definitions
│   │   ├── domain/                    # Domain Layer (Pure Business Logic)
│   │   │   ├── model/                 # NetworkMode, ToggleModeConfig, ControlMethod
│   │   │   ├── repository/            # Repository interfaces
│   │   │   └── usecase/               # Domain Use Cases (Toggle, Mode Check, Compatibility)
│   │   ├── data/                      # Data Layer
│   │   │   ├── repository/            # NetworkControl & Preferences repository implementations
│   │   │   └── source/                # DataStore & Network Control data sources
│   │   ├── service/                   # Android System Services
│   │   │   ├── NetworkTileService.kt            # Smart Quick Settings Tile
│   │   │   ├── RootNetworkControllerService.kt  # Root IPC service
│   │   │   └── ShizukuControllerService.kt      # Shizuku IPC service
│   │   └── di/                        # Hilt DI modules (DataModule, etc.)
│   └── aidl/                          # AIDL definitions for IPC
│
hiddenapi/                             # Android Hidden API stubs module
.github/workflows/
├── build.yml                          # CI build verification
└── release.yml                        # GitHub release automated build
```

## Building from Source

```bash
# Clone repository
git clone https://github.com/markvoronin354/NetworkSwitch.git
cd NetworkSwitch

# Build debug APK
./gradlew assembleDebug

# Run unit test suite
./gradlew testDebugUnitTest
```

## Testing

The codebase includes comprehensive unit tests utilizing **JUnit 4**, **MockK**, and **Kotlinx Coroutines Test**:

```bash
./gradlew test
```

## Contributing

Contributions are welcome!
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Ensure all unit tests pass (`./gradlew test`)
4. Commit changes following standard Kotlin coding guidelines
5. Submit a Pull Request

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

## Credits & Acknowledgments

- **Shizuku**: Non-root system API execution
- **libsu**: Root service management
- **Hilt**: Dependency injection framework
- **Jetpack Compose**: Declarative UI library
