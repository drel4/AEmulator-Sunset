<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**Klasik Android yazılımları — HTC Sense, TouchWiz, MIUI, AOSP — modern bir telefonda. Root ve PC gerekmez.**

[![Version](https://img.shields.io/badge/version-0.0.0.2-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · **🇹🇷 Türkçe** · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator gerçek bir Android 2.3–7.x sistemini doğrudan yazılım dosyasından başlatır: recovery ZIP, Odin arşivi veya Google fabrika imajı. Eski ARM kodu değiştirilmiş QEMU ile çevrilir, çekirdeğin binder’ı taklit edilir, grafikler telefonun GPU’sunu, ses Android’in ses altyapısını kullanır — hepsi sıradan bir uygulamanın içinde.

## ✨ Özellikler

- Neredeyse her biçimi içe aktarma: CWM/TWRP ZIP, Samsung Odin `.tar.md5`, Google fabrika `.tgz`, `system.img`, OTA `system.new.dat.br`
- Üretici arayüzleri olduğu gibi çalışır: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- GL köprüsüyle donanım grafikleri, ses, dokunma ve çoklu dokunma, modern TLS vekilli ağ
- APK, müzik ve fotoğraflar için ortak hafıza kartı klasörü
- 18 dilde Material 3 Expressive arayüz
- Ücretsiz ve açık kaynak (GPL-3.0)

## 📱 Yazılım listesi

Test edilen yazılımların listesi — durumlar, notlar ve indirme bağlantılarıyla — forumumuzda. Orada sonuçlarını paylaşabilir, soru sorabilir ve yeni imajlar bulabilirsin.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/Forumu%20a%C3%A7-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="Forum"/></a>

</div>

## 🚀 Hızlı başlangıç

1. APK’yı [Releases](https://github.com/uxazu/aemulator/releases) sayfasından indirip kurun.
2. [Forumda](https://aeforum.uxazuu.space/) bir yazılım bul ve telefonuna indir.
3. AEmulator’ı açın → **Yazılım ekle** ve dosyayı seçin. İçe aktarma birkaç dakika sürer.
4. **Başlat**’a basın. İlk açılış daha uzundur: sistem uygulamaları optimize eder.
5. ⋮ menüsü: ses, güç düğmesi ve günlük; ⚙️ ayarları ve dili açar.

## 📋 Gereksinimler

- 64 bit ARM telefonda Android 8.0+ (arm64-v8a)
- Yazılım başına yaklaşık 1–3 GB boş alan
- Önerilen: yeni bir Snapdragon / Dimensity / Tensor

## ⚙️ Nasıl çalışır

Her misafir süreç değiştirilmiş kullanıcı kipi QEMU altında çalışır. Bir binder arka plan programı çekirdek sürücüsünün yerini alır, GL köprüsü OpenGL ES çağrılarını GPU’ya iletir, küçük misafir kütüphaneleri (ses HAL’i, audio policy sarmalayıcısı, LD_PRELOAD katmanı) üretici kodunu emülatöre uyarlar. İçe aktarıcı yazılımı okur, boot imajındaki init betiklerini bulur ve servislerin başlama planını kurar.

## 🛠️ Kaynaktan derleme

JDK 17, Android SDK 36 ve NDK r28 gerekir. Misafir kütüphaneleri `native/*/build.sh` betikleriyle derlenir.

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 Teşekkürler

AEmulator, [ilk geliştiricinin](https://t.me/istratii_tech) HTC Desire HD ve HTC One M7 emülatörlerinden doğdu — onun motoru olmadan bu proje olmazdı.

## 💙 Projeyi destekleyin

AEmulator sevdiğiniz bir telefonu geri getirdiyse geliştirmeyi destekleyebilirsiniz:

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💵 USDT (TRC20): `TN5cZFQ6BKPKCJZiUqEQKaifaEdtNUqaBF`
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 Bağlantılar

- 🌐 Web sitesi: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 Forum: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 Telegram kanalı: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 Geliştirici: [uxazu](https://github.com/uxazu)
- 🧬 İlk geliştirici: [t.me/istratii_tech](https://t.me/istratii_tech)

## 📄 Lisans

GPL-3.0. Android, markalar ve yazılımlar sahiplerine aittir.
