<div dir="rtl">

<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**فرم‌ویرهای کلاسیک اندروید — HTC Sense، TouchWiz، MIUI، AOSP — روی گوشی مدرن. بدون روت و بدون رایانه.**

[![Version](https://img.shields.io/badge/version-0.0.0.1-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · **🇮🇷 فارسی** · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator یک سیستم واقعی اندروید 2.3 تا 4.4 را مستقیم از فایل فرم‌ویر بوت می‌کند: ZIP ریکاوری، آرشیو Odin یا ایمیج کارخانهٔ گوگل. کد قدیمی ARM با QEMU اصلاح‌شده ترجمه می‌شود، binder هسته شبیه‌سازی می‌شود، گرافیک با GPU گوشی و صدا با سامانهٔ صوتی اندروید پخش می‌شود — همه درون یک برنامهٔ معمولی.

## ✨ ویژگی‌ها

- درون‌ریزی تقریباً همهٔ قالب‌ها: ‏ZIP ‏CWM/TWRP، ‏Odin ‏`.tar.md5` سامسونگ، ایمیج کارخانهٔ گوگل ‏`.tgz`، ‏`system.img`، ‏OTA ‏`system.new.dat.br`
- رابط‌های سازندگان همان‌طور که هستند کار می‌کنند: HTC Sense، Samsung TouchWiz، MIUI، AOSP
- گرافیک سخت‌افزاری از طریق پل GL، صدا، لمس و چندلمسی، شبکه با پراکسی TLS مدرن
- پوشهٔ مشترک کارت حافظه برای APK، موسیقی و عکس
- رابط Material 3 Expressive به 18 زبان
- رایگان و متن‌باز (GPL-3.0)

## 📱 فهرست فرم‌ویرها

فهرست فرم‌ویرهای آزموده — با وضعیت، یادداشت و پیوند دانلود — در انجمن ما است. آنجا نتایج خود را به اشتراک بگذارید، بپرسید و ایمیج‌های تازه پیدا کنید.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/%D8%A8%D8%A7%D8%B2%20%DA%A9%D8%B1%D8%AF%D9%86%20%D8%A7%D9%86%D8%AC%D9%85%D9%86-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="انجمن"/></a>

</div>

## 🚀 شروع سریع

1. فایل APK را از [Releases](https://github.com/uxazu/aemulator/releases) دانلود و نصب کنید.
2. فرم‌ویری را در [انجمن](https://aeforum.uxazuu.space/) پیدا کنید و روی گوشی دانلود کنید.
3. AEmulator را باز کنید ← **افزودن فرم‌ویر** و فایل را انتخاب کنید. درون‌ریزی چند دقیقه طول می‌کشد.
4. **اجرا** را بزنید. بوت اول کندتر است: سیستم برنامه‌ها را بهینه می‌کند.
5. منوی ⋮ برای صدا، دکمهٔ پاور و گزارش؛ دکمهٔ ⚙️ برای تنظیمات و زبان.

## 📋 نیازمندی‌ها

- اندروید 8.0+ روی گوشی ARM ‏64 بیتی (arm64-v8a)
- حدود 1 تا 3 گیگابایت فضای آزاد برای هر فرم‌ویر
- پیشنهاد: Snapdragon / Dimensity / Tensor جدید

## ⚙️ چگونه کار می‌کند

هر فرایند مهمان زیر یک QEMU اصلاح‌شده در حالت کاربر اجرا می‌شود. سرویس binder جای درایور هسته را می‌گیرد، پل GL فراخوانی‌های OpenGL ES را به GPU می‌فرستد و کتابخانه‌های کوچک مهمان (HAL صوتی، پوشش audio policy، لایهٔ LD_PRELOAD) کد سازندگان را با شبیه‌ساز سازگار می‌کنند. درون‌ریز فرم‌ویر را می‌خواند، اسکریپت‌های init را در ایمیج boot پیدا می‌کند و برنامهٔ اجرای سرویس‌ها را می‌سازد.

## 🛠️ ساخت از کد منبع

به JDK 17، Android SDK 36 و NDK r28 نیاز دارید. کتابخانه‌های مهمان با `native/*/build.sh` ساخته می‌شوند.

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 سپاس

AEmulator از شبیه‌سازهای HTC Desire HD و HTC One M7 ‏[سازندهٔ نخست](https://t.me/istratii_tech) رشد کرد — بدون موتور او این پروژه وجود نداشت.

## 💙 حمایت از پروژه

اگر AEmulator گوشی محبوبتان را برگرداند، می‌توانید از توسعه حمایت کنید:

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💵 USDT (TRC20): `TN5cZFQ6BKPKCJZiUqEQKaifaEdtNUqaBF`
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 پیوندها

- 🌐 وب‌سایت: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 انجمن: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 کانال تلگرام: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 سازنده: [uxazu](https://github.com/uxazu)
- 🧬 سازندهٔ نخست: [t.me/istratii_tech](https://t.me/istratii_tech)

## 📄 مجوز

GPL-3.0. اندروید، نشان‌های تجاری و فرم‌ویرها متعلق به صاحبانشان‌اند.

</div>
