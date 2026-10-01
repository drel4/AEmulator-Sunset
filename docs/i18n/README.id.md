<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator Sunset logo"/>

# AEmulator Sunset

**Firmware Android klasik — HTC Sense, TouchWiz, MIUI, AOSP — di ponsel modern. Tanpa root, tanpa PC.**

[![Version](https://img.shields.io/badge/version-0.0.0.3--sunset.2-F4511E?style=for-the-badge)](https://github.com/drel4/AEmulator-Sunset/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-F4511E?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/drel4/AEmulator-Sunset) [![Fork](https://img.shields.io/badge/fork-drel4%2FAEmulator--Sunset-F4511E?style=for-the-badge&logo=github)](https://github.com/drel4/AEmulator-Sunset)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · **🇮🇩 Indonesia** · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator Sunset mem-boot sistem Android 2.3–7.x asli langsung dari file firmware: ZIP recovery, arsip Odin, atau factory image Google. Kode ARM lama diterjemahkan oleh QEMU yang dimodifikasi, binder kernel diemulasikan, grafis memakai GPU ponsel, dan suara lewat sistem audio Android — semuanya di dalam aplikasi biasa.

## ✨ Fitur

- Impor hampir semua format: ZIP CWM/TWRP, Odin `.tar.md5` Samsung, factory image Google `.tgz`, `system.img`, OTA `system.new.dat.br`
- Tampilan pabrikan berjalan apa adanya: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Grafis hardware lewat jembatan GL, suara, sentuh dan multisentuh, jaringan dengan proxy TLS modern
- Folder kartu memori bersama untuk APK, musik, dan foto
- Antarmuka Material 3 Expressive dalam 18 bahasa
- Gratis dan sumber terbuka (GPL-3.0)

## 🚀 Mulai cepat

1. Unduh APK dari [Releases](https://github.com/drel4/AEmulator-Sunset/releases) lalu pasang.
2. Buka AEmulator Sunset → **Tambah firmware** lalu pilih file. Impor butuh beberapa menit.
3. Tekan **Mulai**. Boot pertama lebih lambat: sistem mengoptimalkan aplikasi.
4. Menu ⋮ untuk volume, tombol daya, dan log; ⚙️ membuka setelan dan bahasa.

## 📋 Persyaratan

- Android 8.0+ di ponsel ARM 64-bit (arm64-v8a)
- Sekitar 1–3 GB ruang kosong per firmware
- Disarankan Snapdragon / Dimensity / Tensor terbaru

## ⚙️ Cara kerja

Setiap proses tamu berjalan di QEMU mode pengguna yang dimodifikasi. Daemon binder menggantikan driver kernel, jembatan GL meneruskan panggilan OpenGL ES ke GPU, dan pustaka tamu kecil (HAL audio, pembungkus audio policy, shim LD_PRELOAD) menyesuaikan kode pabrikan dengan emulator. Importir membaca firmware, menemukan skrip init di image boot, dan menyusun rencana mulai layanan.

## 🛠️ Membangun dari sumber

Butuh JDK 17, Android SDK 36, dan NDK r28. Pustaka tamu dibangun dengan `native/*/build.sh`.

```bash
git clone https://github.com/drel4/AEmulator-Sunset.git
cd AEmulator-Sunset
./gradlew copyReleaseApks
```

## 🙏 Kredit

AEmulator Sunset tumbuh dari emulator HTC Desire HD dan HTC One M7 karya [pembuat asli](https://t.me/istratiit_ech) — tanpa mesinnya proyek ini tidak akan ada.

This is a modified fork of [uxazu/AEmulator](https://github.com/uxazu/AEmulator). Fork changes are documented in [NOTICE](../../NOTICE.md).

## 🔗 Tautan

- 🧬 Fork: [drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset)
- ↑ Upstream: [uxazu/AEmulator](https://github.com/uxazu/AEmulator)
- 👤 Pembuat: [drel4](https://github.com/drel4)
- 🧬 Pembuat asli: [t.me/istratiit_ech](https://t.me/istratiit_ech)

## 📄 Lisensi

GPL-3.0. Android, merek dagang, dan firmware milik pemiliknya.
