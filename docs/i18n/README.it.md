<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator Sunset logo"/>

# AEmulator Sunset

**Firmware Android classici — HTC Sense, TouchWiz, MIUI, AOSP — su un telefono moderno. Senza root, senza PC.**

[![Version](https://img.shields.io/badge/version-0.0.0.3--sunset.2-F4511E?style=for-the-badge)](https://github.com/drel4/AEmulator-Sunset/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-F4511E?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/drel4/AEmulator-Sunset) [![Fork](https://img.shields.io/badge/fork-drel4%2FAEmulator--Sunset-F4511E?style=for-the-badge&logo=github)](https://github.com/drel4/AEmulator-Sunset)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · **🇮🇹 Italiano** · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator Sunset avvia un vero sistema Android 2.3–7.x direttamente da un file firmware: ZIP per recovery, archivio Odin o immagine di fabbrica Google. Il vecchio codice ARM passa da un QEMU modificato, il binder del kernel è emulato, la grafica usa la GPU del telefono e l’audio passa per lo stack audio di Android — tutto in una normale app.

## ✨ Funzionalità

- Importa quasi ogni formato: ZIP CWM/TWRP, Odin `.tar.md5` di Samsung, immagine di fabbrica Google `.tgz`, `system.img`, OTA `system.new.dat.br`
- Le interfacce dei produttori funzionano così come sono: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Grafica hardware tramite il ponte GL, audio, tocco e multitouch, rete con proxy TLS moderno
- Cartella condivisa della scheda di memoria per APK, musica e foto
- Interfaccia Material 3 Expressive in 18 lingue
- Gratuito e open source (GPL-3.0)

## 🚀 Avvio rapido

1. Scarica l’APK da [Releases](https://github.com/drel4/AEmulator-Sunset/releases) e installalo.
2. Apri AEmulator Sunset → **Aggiungi firmware** e scegli il file. L’importazione richiede qualche minuto.
3. Premi **Avvia**. Il primo avvio è più lento: il sistema ottimizza le app.
4. Menu ⋮ per volume, tasto di accensione e registro; ⚙️ apre impostazioni e lingua.

## 📋 Requisiti

- Android 8.0+ su telefono ARM a 64 bit (arm64-v8a)
- Circa 1–3 GB liberi per firmware
- Consigliato: Snapdragon / Dimensity / Tensor recente

## ⚙️ Come funziona

Ogni processo ospite gira sotto un QEMU user-mode modificato. Un demone binder sostituisce il driver del kernel, un ponte GL inoltra le chiamate OpenGL ES alla GPU e piccole librerie ospiti (HAL audio, wrapper audio policy, shim LD_PRELOAD) adattano il codice dei produttori all’emulatore. L’importatore legge il firmware, trova gli script init nell’immagine boot e crea il piano di avvio dei servizi.

## 🛠️ Compilare dai sorgenti

Servono JDK 17, Android SDK 36 e NDK r28. Le librerie ospiti si compilano con `native/*/build.sh`.

```bash
git clone https://github.com/drel4/AEmulator-Sunset.git
cd AEmulator-Sunset
./gradlew copyReleaseApks
```

## 🙏 Ringraziamenti

AEmulator Sunset nasce dagli emulatori HTC Desire HD e HTC One M7 dell’[autore originale](https://t.me/istratiit_ech); senza il suo motore questo progetto non esisterebbe.

This is a modified fork of [uxazu/AEmulator](https://github.com/uxazu/AEmulator). Fork changes are documented in [NOTICE](../../NOTICE.md).

## 🔗 Link

- 🧬 Fork: [drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset)
- ↑ Upstream: [uxazu/AEmulator](https://github.com/uxazu/AEmulator)
- 👤 Autore: [drel4](https://github.com/drel4)
- 🧬 Autore originale: [t.me/istratiit_ech](https://t.me/istratiit_ech)

## 📄 Licenza

GPL-3.0. Android, i marchi e i firmware appartengono ai rispettivi proprietari.
