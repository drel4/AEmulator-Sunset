<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator Sunset logo"/>

# AEmulator Sunset

**Firmware clásico de Android — HTC Sense, TouchWiz, MIUI, AOSP — en un teléfono moderno. Sin root, sin PC.**

[![Version](https://img.shields.io/badge/version-0.0.0.3--sunset.2-F4511E?style=for-the-badge)](https://github.com/drel4/AEmulator-Sunset/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-F4511E?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/drel4/AEmulator-Sunset) [![Fork](https://img.shields.io/badge/fork-drel4%2FAEmulator--Sunset-F4511E?style=for-the-badge&logo=github)](https://github.com/drel4/AEmulator-Sunset)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · **🇪🇸 Español** · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator Sunset arranca un sistema Android 2.3–7.x real directamente desde un archivo de firmware: ZIP de recovery, archivo de Odin o imagen de fábrica de Google. El código ARM antiguo se traduce con un QEMU modificado, el binder del kernel se emula, los gráficos usan la GPU del teléfono y el sonido pasa por la pila de audio de Android — todo dentro de una app normal.

## ✨ Características

- Importa casi cualquier formato: ZIP CWM/TWRP, Odin `.tar.md5` de Samsung, imagen de fábrica `.tgz` de Google, `system.img`, OTA `system.new.dat.br`
- Las capas de los fabricantes funcionan tal cual: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Gráficos por hardware con el puente GL, sonido, táctil y multitáctil, red con proxy TLS moderno
- Carpeta compartida de tarjeta de memoria para APK, música y fotos
- Interfaz Material 3 Expressive en 18 idiomas
- Gratis y de código abierto (GPL-3.0)

## 🚀 Inicio rápido

1. Descarga el APK desde [Releases](https://github.com/drel4/AEmulator-Sunset/releases) e instálalo.
2. Abre AEmulator Sunset → **Añadir firmware** y elige el archivo. La importación tarda unos minutos.
3. Pulsa **Iniciar**. El primer arranque es más lento: el sistema optimiza las apps.
4. Menú ⋮ para volumen, botón de encendido y registro; ⚙️ abre ajustes e idioma.

## 📋 Requisitos

- Android 8.0+ en un teléfono ARM de 64 bits (arm64-v8a)
- Unos 1–3 GB libres por firmware
- Se recomienda un Snapdragon / Dimensity / Tensor reciente

## ⚙️ Cómo funciona

Cada proceso invitado se ejecuta con un QEMU de modo usuario modificado. Un demonio binder sustituye al controlador del kernel, un puente GL envía las llamadas OpenGL ES a la GPU y pequeñas bibliotecas invitadas (HAL de audio, envoltorio de audio policy, shim LD_PRELOAD) adaptan el código del fabricante al emulador. El importador lee el firmware, encuentra los scripts init en la imagen boot y crea el plan de arranque de los servicios.

## 🛠️ Compilar desde el código

Necesitas JDK 17, Android SDK 36 y NDK r28. Las bibliotecas del invitado se compilan con `native/*/build.sh`.

```bash
git clone https://github.com/drel4/AEmulator-Sunset.git
cd AEmulator-Sunset
./gradlew copyReleaseApks
```

## 🙏 Créditos

AEmulator Sunset nació de los emuladores de HTC Desire HD y HTC One M7 del [autor original](https://t.me/istratiit_ech); sin su motor este proyecto no existiría.

This is a modified fork of [uxazu/AEmulator](https://github.com/uxazu/AEmulator). Fork changes are documented in [NOTICE](../../NOTICE.md).

## 🔗 Enlaces

- 🧬 Fork: [drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset)
- ↑ Upstream: [uxazu/AEmulator](https://github.com/uxazu/AEmulator)
- 👤 Autor: [drel4](https://github.com/drel4)
- 🧬 Autor original: [t.me/istratiit_ech](https://t.me/istratiit_ech)

## 📄 Licencia

GPL-3.0. Android, las marcas y el firmware pertenecen a sus dueños.
