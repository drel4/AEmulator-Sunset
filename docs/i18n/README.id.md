<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**Firmware Android klasik — HTC Sense, TouchWiz, MIUI, AOSP — di ponsel modern. Tanpa root, tanpa PC.**

[![Version](https://img.shields.io/badge/version-0.0.0.2-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · **🇮🇩 Indonesia** · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator mem-boot sistem Android 2.3–7.x asli langsung dari file firmware: ZIP recovery, arsip Odin, atau factory image Google. Kode ARM lama diterjemahkan oleh QEMU yang dimodifikasi, binder kernel diemulasikan, grafis memakai GPU ponsel, dan suara lewat sistem audio Android — semuanya di dalam aplikasi biasa.

## ✨ Fitur

- Impor hampir semua format: ZIP CWM/TWRP, Odin `.tar.md5` Samsung, factory image Google `.tgz`, `system.img`, OTA `system.new.dat.br`
- Tampilan pabrikan berjalan apa adanya: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Grafis hardware lewat jembatan GL, suara, sentuh dan multisentuh, jaringan dengan proxy TLS modern
- Folder kartu memori bersama untuk APK, musik, dan foto
- Antarmuka Material 3 Expressive dalam 18 bahasa
- Gratis dan sumber terbuka (GPL-3.0)

## 📱 Daftar firmware

Daftar firmware yang sudah diperiksa — lengkap dengan status, catatan, dan tautan unduhan — ada di forum kami. Bagikan hasilmu, bertanya, dan temukan image baru di sana.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/Buka%20forum-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="Forum"/></a>

</div>

## 🚀 Mulai cepat

1. Unduh APK dari [Releases](https://github.com/uxazu/aemulator/releases) lalu pasang.
2. Cari firmware di [forum](https://aeforum.uxazuu.space/) lalu unduh ke ponsel.
3. Buka AEmulator → **Tambah firmware** lalu pilih file. Impor butuh beberapa menit.
4. Tekan **Mulai**. Boot pertama lebih lambat: sistem mengoptimalkan aplikasi.
5. Menu ⋮ untuk volume, tombol daya, dan log; ⚙️ membuka setelan dan bahasa.

## 📋 Persyaratan

- Android 8.0+ di ponsel ARM 64-bit (arm64-v8a)
- Sekitar 1–3 GB ruang kosong per firmware
- Disarankan Snapdragon / Dimensity / Tensor terbaru

## ⚙️ Cara kerja

Setiap proses tamu berjalan di QEMU mode pengguna yang dimodifikasi. Daemon binder menggantikan driver kernel, jembatan GL meneruskan panggilan OpenGL ES ke GPU, dan pustaka tamu kecil (HAL audio, pembungkus audio policy, shim LD_PRELOAD) menyesuaikan kode pabrikan dengan emulator. Importir membaca firmware, menemukan skrip init di image boot, dan menyusun rencana mulai layanan.

## 🛠️ Membangun dari sumber

Butuh JDK 17, Android SDK 36, dan NDK r28. Pustaka tamu dibangun dengan `native/*/build.sh`.

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 Kredit

AEmulator tumbuh dari emulator HTC Desire HD dan HTC One M7 karya [pembuat asli](https://t.me/istratiit_ech) — tanpa mesinnya proyek ini tidak akan ada.

## 💙 Dukung proyek

Jika AEmulator mengembalikan ponsel kesayangan Anda, Anda bisa mendukung pengembangan:

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 Tautan

- 🌐 Situs web: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 Forum: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 Kanal Telegram: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 Pembuat: [uxazu](https://github.com/uxazu)
- 🧬 Pembuat asli: [t.me/istratiit_ech](https://t.me/istratiit_ech)

## 📄 Lisensi

GPL-3.0. Android, merek dagang, dan firmware milik pemiliknya.
