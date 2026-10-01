<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator Sunset logo"/>

# AEmulator Sunset

**Старые прошивки Android — HTC Sense, TouchWiz, MIUI, AOSP — на современном телефоне. Без root и без ПК.**

[![Version](https://img.shields.io/badge/version-0.0.0.3--sunset.1-F4511E?style=for-the-badge)](https://github.com/drel4/AEmulator-Sunset/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-F4511E?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/drel4/AEmulator-Sunset) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Fork](https://img.shields.io/badge/fork-drel4%2FAEmulator--Sunset-F4511E?style=for-the-badge&logo=github)](https://github.com/drel4/AEmulator-Sunset)

[🇬🇧 English](../../README.md) · **🇷🇺 Русский** · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator Sunset загружает настоящую систему Android 2.3–7.x прямо из файла прошивки: ZIP для рекавери, архива Odin или factory-образа Google. Старый ARM-код транслирует доработанный QEMU, binder ядра эмулируется, графика идёт через GPU телефона, звук — через аудиосистему Android. Всё работает внутри обычного приложения.

## ✨ Возможности

- Импорт почти любых форматов: ZIP для CWM/TWRP, Odin `.tar.md5` от Samsung, factory `.tgz` от Google, `system.img`, OTA `system.new.dat.br`
- Оболочки производителей работают как есть: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Аппаратная графика через GL-мост, звук, касания и мультитач, сеть с современным TLS-прокси
- Общая папка «карты памяти» для APK, музыки и фото
- Интерфейс Material 3 Expressive на 18 языках
- Бесплатно и с открытым кодом (GPL-3.0)

## 📱 Список прошивок

Список проверенных прошивок — со статусами, заметками и ссылками на скачивание — теперь на нашем форуме. Там же можно делиться результатами, задавать вопросы и находить новые образы.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/%D0%9E%D1%82%D0%BA%D1%80%D1%8B%D1%82%D1%8C%20%D1%84%D0%BE%D1%80%D1%83%D0%BC-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="Форум"/></a>

</div>

## 🚀 Быстрый старт

1. Скачайте APK из [Releases](https://github.com/drel4/AEmulator-Sunset/releases) и установите.
2. Найдите прошивку на [форуме](https://aeforum.uxazuu.space/) и скачайте её на телефон.
3. Откройте AEmulator Sunset → **Добавить прошивку** и выберите файл. Импорт занимает несколько минут.
4. Нажмите **Запустить**. Первая загрузка дольше: система оптимизирует приложения.
5. Меню ⋮ — громкость, кнопка питания и журнал; кнопка ⚙️ — настройки приложения и язык.

## 📋 Требования

- Android 8.0+ на 64-битном ARM-телефоне (arm64-v8a)
- Около 1–3 ГБ свободного места на прошивку
- Рекомендуется современный Snapdragon / Dimensity / Tensor

## ⚙️ Как это устроено

Каждый процесс гостя работает под доработанным QEMU в режиме пользователя. Демон binder заменяет драйвер ядра, GL-мост передаёт вызовы OpenGL ES на GPU телефона, а небольшие гостевые библиотеки (звуковой HAL, обёртка audio policy, прослойка LD_PRELOAD) подгоняют код производителей под эмулятор. Импортёр читает прошивку, находит init-скрипты в boot-образе и строит план запуска системных служб.

## 🛠️ Сборка из исходников

Нужны JDK 17, Android SDK 36 и NDK r28. Гостевые библиотеки собираются скриптами `native/*/build.sh`.

```bash
git clone https://github.com/drel4/AEmulator-Sunset.git
cd AEmulator-Sunset
./gradlew copyReleaseApks
```

## 🙏 Благодарности

AEmulator Sunset вырос из эмуляторов HTC Desire HD и HTC One M7 [первого автора](https://t.me/istratiit_ech) — без его движка проекта бы не было.

This is a modified fork of [uxazu/AEmulator](https://github.com/uxazu/AEmulator). Fork changes are documented in [NOTICE](../../NOTICE.md).

## 🔗 Ссылки

- 🧬 Fork: [drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset)
- ↑ Upstream: [uxazu/AEmulator](https://github.com/uxazu/AEmulator)
- 👤 Автор: [drel4](https://github.com/drel4)
- 🧬 Первый автор: [t.me/istratiit_ech](https://t.me/istratiit_ech)

## 📄 Лицензия

GPL-3.0. Android, товарные знаки и прошивки принадлежат их владельцам.
