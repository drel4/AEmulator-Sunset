<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**Klasyczne firmware Androida — HTC Sense, TouchWiz, MIUI, AOSP — na nowoczesnym telefonie. Bez roota, bez PC.**

[![Version](https://img.shields.io/badge/version-0.0.0.2-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · **🇵🇱 Polski** · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator uruchamia prawdziwy system Android 2.3–7.x prosto z pliku firmware: ZIP do recovery, archiwum Odin lub obraz fabryczny Google. Stary kod ARM tłumaczy zmodyfikowane QEMU, binder jądra jest emulowany, grafika korzysta z GPU telefonu, a dźwięk z systemu audio Androida — wszystko w zwykłej aplikacji.

## ✨ Funkcje

- Import niemal każdego formatu: ZIP CWM/TWRP, Odin `.tar.md5` Samsunga, obraz fabryczny Google `.tgz`, `system.img`, OTA `system.new.dat.br`
- Nakładki producentów działają bez zmian: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Sprzętowa grafika przez mostek GL, dźwięk, dotyk i multitouch, sieć z nowoczesnym proxy TLS
- Wspólny folder karty pamięci na APK, muzykę i zdjęcia
- Interfejs Material 3 Expressive w 18 językach
- Za darmo i open source (GPL-3.0)

## 📱 Lista firmware

Lista sprawdzonych firmware — ze statusami, uwagami i linkami do pobrania — jest na naszym forum. Możesz tam dzielić się wynikami, zadawać pytania i znajdować nowe obrazy.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/Otw%C3%B3rz%20forum-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="Forum"/></a>

</div>

## 🚀 Szybki start

1. Pobierz APK z [Releases](https://github.com/uxazu/aemulator/releases) i zainstaluj.
2. Znajdź firmware na [forum](https://aeforum.uxazuu.space/) i pobierz je na telefon.
3. Otwórz AEmulator → **Dodaj firmware** i wybierz plik. Import trwa kilka minut.
4. Naciśnij **Uruchom**. Pierwszy start trwa dłużej: system optymalizuje aplikacje.
5. Menu ⋮ — głośność, przycisk zasilania i dziennik; ⚙️ — ustawienia i język.

## 📋 Wymagania

- Android 8.0+ na 64-bitowym telefonie ARM (arm64-v8a)
- Około 1–3 GB wolnego miejsca na firmware
- Zalecany nowy Snapdragon / Dimensity / Tensor

## ⚙️ Jak to działa

Każdy proces gościa działa pod zmodyfikowanym QEMU w trybie użytkownika. Demon binder zastępuje sterownik jądra, mostek GL przekazuje wywołania OpenGL ES do GPU, a małe biblioteki gościa (HAL audio, nakładka audio policy, shim LD_PRELOAD) dopasowują kod producentów do emulatora. Importer czyta firmware, znajduje skrypty init w obrazie boot i tworzy plan startu usług.

## 🛠️ Budowanie ze źródeł

Potrzebne są JDK 17, Android SDK 36 i NDK r28. Biblioteki gościa buduje się skryptami `native/*/build.sh`.

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 Podziękowania

AEmulator wyrósł z emulatorów HTC Desire HD i HTC One M7 [pierwotnego autora](https://t.me/istratii_tech) — bez jego silnika tego projektu by nie było.

## 💙 Wesprzyj projekt

Jeśli AEmulator przywrócił ci ulubiony telefon, możesz wesprzeć rozwój:

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💵 USDT (TRC20): `TN5cZFQ6BKPKCJZiUqEQKaifaEdtNUqaBF`
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 Linki

- 🌐 Strona: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 Forum: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 Kanał Telegram: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 Autor: [uxazu](https://github.com/uxazu)
- 🧬 Pierwotny autor: [t.me/istratii_tech](https://t.me/istratii_tech)

## 📄 Licencja

GPL-3.0. Android, znaki towarowe i firmware należą do ich właścicieli.
