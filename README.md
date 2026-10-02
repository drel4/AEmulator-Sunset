<div align="center">

<img src="docs/assets/logo.png" width="128" alt="AEmulator Sunset logo"/>

# AEmulator Sunset

**Run classic Android firmware — HTC Sense, TouchWiz, MIUI, AOSP — on a modern phone. No root, no PC.**

[![Version](https://img.shields.io/badge/version-0.0.0.3--sunset.23-F4511E?style=for-the-badge)](https://github.com/drel4/AEmulator-Sunset/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-F4511E?style=for-the-badge)](LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/drel4/AEmulator-Sunset) [![Fork](https://img.shields.io/badge/fork-drel4%2FAEmulator--Sunset-F4511E?style=for-the-badge&logo=github)](https://github.com/drel4/AEmulator-Sunset)

**🇬🇧 English** · [🇷🇺 Русский](docs/i18n/README.ru.md) · [🇺🇦 Українська](docs/i18n/README.uk.md) · [🇩🇪 Deutsch](docs/i18n/README.de.md) · [🇫🇷 Français](docs/i18n/README.fr.md) · [🇪🇸 Español](docs/i18n/README.es.md) · [🇧🇷 Português](docs/i18n/README.pt-BR.md) · [🇮🇹 Italiano](docs/i18n/README.it.md) · [🇵🇱 Polski](docs/i18n/README.pl.md) · [🇹🇷 Türkçe](docs/i18n/README.tr.md) · [🇸🇦 العربية](docs/i18n/README.ar.md) · [🇮🇷 فارسی](docs/i18n/README.fa.md) · [🇮🇳 हिन्दी](docs/i18n/README.hi.md) · [🇮🇩 Indonesia](docs/i18n/README.id.md) · [🇻🇳 Tiếng Việt](docs/i18n/README.vi.md) · [🇨🇳 简体中文](docs/i18n/README.zh-CN.md) · [🇯🇵 日本語](docs/i18n/README.ja.md) · [🇰🇷 한국어](docs/i18n/README.ko.md)

</div>

---

AEmulator Sunset boots a real Android 2.3–7.x system image straight from a firmware file you already have: a recovery ZIP, an Odin archive or a Google factory image. It translates the old ARM code with a patched QEMU, emulates the kernel’s binder, draws with your phone’s GPU and plays sound through Android’s audio stack — everything runs inside a normal app.

## ✨ Features

- Import almost any firmware format: CWM/TWRP ZIP, Samsung Odin `.tar.md5`, Google factory `.tgz`, `system.img`, OTA `system.new.dat.br`
- Vendor skins work as shipped: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Hardware graphics through the GL bridge, sound, touch and multitouch, network with a modern TLS proxy
- Shared memory-card folder for APKs, music and photos
- Material 3 Expressive interface in 18 languages
- Free and open source (GPL-3.0)

## 🚀 Quick start

1. Download the APK from [Releases](https://github.com/drel4/AEmulator-Sunset/releases) and install it.
2. Open AEmulator Sunset → **Add firmware** and pick the file. Import takes a few minutes.
3. Press **Start**. The first boot is slower: the system optimises its apps.
4. Use the ⋮ menu for volume, power button and logs; the ⚙️ button opens app settings and language.

## 📋 Requirements

- Android 8.0+ on a 64-bit ARM phone (arm64-v8a)
- About 1–3 GB of free space per firmware
- A recent Snapdragon / Dimensity / Tensor chip is recommended

## ⚙️ How it works

Each guest process runs under a patched user-mode QEMU. A binder daemon replaces the kernel driver, a GL bridge forwards OpenGL ES calls to the phone’s GPU, and small guest libraries (audio HAL, audio policy wrapper, LD_PRELOAD shim) adapt vendor code to the emulator. The importer reads the firmware, finds the init scripts in its boot image and builds a start plan for system services.

## 🛠️ Build from source

The tested app release build uses JDK 21 and Android SDK 36. See
[build/source instructions](docs/build-source.md), including native toolchain
details and signing. **Inherited engine prebuilts do not yet have verified
complete source/build provenance**; see [the audit](docs/license-audit.md).

```bash
git clone https://github.com/drel4/AEmulator-Sunset.git
cd AEmulator-Sunset
./gradlew copyReleaseApks
```

## 🙏 Credits

AEmulator Sunset grew out of the HTC Desire HD and HTC One M7 emulators by [the original author](https://t.me/istratiit_ech) — their engine made this project possible.

This is a modified fork of [uxazu/AEmulator](https://github.com/uxazu/AEmulator). Fork changes are documented in [NOTICE](NOTICE.md).

## 🔗 Links

- 🧬 Fork: [drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset)
- ↑ Upstream: [uxazu/AEmulator](https://github.com/uxazu/AEmulator)
- 👤 Author: [drel4](https://github.com/drel4)
- 🧬 Original author: [t.me/istratiit_ech](https://t.me/istratiit_ech)

## 📄 License

GPL-3.0. Android, trademarks and firmware belong to their owners.

See [dated modification notices](NOTICE.md) and the
[outstanding source/licensing audit](docs/license-audit.md). The GPL label is
not a certification that every bundled prebuilt has complete matching source.
