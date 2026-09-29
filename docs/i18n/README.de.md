<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**Klassische Android-Firmware — HTC Sense, TouchWiz, MIUI, AOSP — auf einem modernen Handy. Ohne Root, ohne PC.**

[![Version](https://img.shields.io/badge/version-0.0.0.2-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · **🇩🇪 Deutsch** · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator startet ein echtes Android-2.3–7.x-System direkt aus einer Firmware-Datei: Recovery-ZIP, Odin-Archiv oder Google-Factory-Image. Der alte ARM-Code läuft über ein angepasstes QEMU, der Binder des Kernels wird emuliert, die Grafik läuft über die GPU des Handys und der Ton über Androids Audiosystem — alles in einer normalen App.

## ✨ Funktionen

- Import fast aller Formate: CWM/TWRP-ZIP, Samsung Odin `.tar.md5`, Google Factory `.tgz`, `system.img`, OTA `system.new.dat.br`
- Hersteller-Oberflächen laufen unverändert: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Hardwaregrafik über die GL-Brücke, Ton, Touch und Multitouch, Netzwerk mit modernem TLS-Proxy
- Gemeinsamer Speicherkarten-Ordner für APKs, Musik und Fotos
- Material-3-Expressive-Oberfläche in 18 Sprachen
- Kostenlos und quelloffen (GPL-3.0)

## 📱 Firmware-Liste

Die Liste geprüfter Firmwares — mit Status, Hinweisen und Download-Links — findest du in unserem Forum. Dort kannst du Ergebnisse teilen, Fragen stellen und neue Images entdecken.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/Forum%20%C3%B6ffnen-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="Forum"/></a>

</div>

## 🚀 Schnellstart

1. APK unter [Releases](https://github.com/uxazu/aemulator/releases) herunterladen und installieren.
2. Such dir im [Forum](https://aeforum.uxazuu.space/) eine Firmware aus und lade sie aufs Handy.
3. AEmulator öffnen → **Firmware hinzufügen** und die Datei wählen. Der Import dauert einige Minuten.
4. **Starten** drücken. Der erste Start dauert länger: Das System optimiert Apps.
5. Menü ⋮ für Lautstärke, Ein/Aus und Protokoll; ⚙️ öffnet Einstellungen und Sprache.

## 📋 Voraussetzungen

- Android 8.0+ auf einem 64-Bit-ARM-Handy (arm64-v8a)
- Etwa 1–3 GB freier Speicher pro Firmware
- Empfohlen: aktueller Snapdragon / Dimensity / Tensor

## ⚙️ So funktioniert es

Jeder Gastprozess läuft unter einem angepassten User-Mode-QEMU. Ein Binder-Daemon ersetzt den Kernel-Treiber, eine GL-Brücke leitet OpenGL-ES-Aufrufe an die GPU weiter, und kleine Gast-Bibliotheken (Audio-HAL, Audio-Policy-Wrapper, LD_PRELOAD-Shim) passen Herstellercode an den Emulator an. Der Importer liest die Firmware, findet die Init-Skripte im Boot-Image und erstellt einen Startplan für die Systemdienste.

## 🛠️ Aus dem Quellcode bauen

Benötigt werden JDK 17, Android SDK 36 und NDK r28. Die Gast-Bibliotheken werden mit `native/*/build.sh` gebaut.

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 Dank

AEmulator ist aus den Emulatoren für HTC Desire HD und HTC One M7 [des Originalautors](https://t.me/istratiit_ech) hervorgegangen — ohne seine Engine gäbe es dieses Projekt nicht.

## 💙 Projekt unterstützen

Wenn AEmulator dir ein geliebtes Handy zurückgebracht hat, kannst du die Entwicklung unterstützen:

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 Links

- 🌐 Website: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 Forum: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 Telegram-Kanal: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 Autor: [uxazu](https://github.com/uxazu)
- 🧬 Originalautor: [t.me/istratiit_ech](https://t.me/istratiit_ech)

## 📄 Lizenz

GPL-3.0. Android, Marken und Firmware gehören ihren Inhabern.
