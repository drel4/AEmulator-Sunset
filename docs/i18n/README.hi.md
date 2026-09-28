<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**पुराने Android फ़र्मवेयर — HTC Sense, TouchWiz, MIUI, AOSP — आधुनिक फ़ोन पर। बिना root, बिना PC।**

[![Version](https://img.shields.io/badge/version-0.0.0.1-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · **🇮🇳 हिन्दी** · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator सीधे फ़र्मवेयर फ़ाइल से असली Android 2.3–7.x सिस्टम बूट करता है: रिकवरी ZIP, Odin आर्काइव या Google फ़ैक्टरी इमेज। पुराना ARM कोड संशोधित QEMU से चलता है, कर्नेल का binder एमुलेट होता है, ग्राफ़िक्स फ़ोन के GPU से और आवाज़ Android के ऑडियो सिस्टम से चलती है — सब कुछ एक सामान्य ऐप के भीतर।

## ✨ विशेषताएँ

- लगभग हर फ़ॉर्मेट आयात करें: CWM/TWRP ZIP, Samsung Odin `.tar.md5`, Google फ़ैक्टरी `.tgz`, `system.img`, OTA `system.new.dat.br`
- कंपनियों के स्किन जैसे हैं वैसे चलते हैं: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- GL ब्रिज से हार्डवेयर ग्राफ़िक्स, आवाज़, टच और मल्टीटच, आधुनिक TLS प्रॉक्सी के साथ नेटवर्क
- APK, संगीत और फ़ोटो के लिए साझा मेमोरी कार्ड फ़ोल्डर
- 18 भाषाओं में Material 3 Expressive इंटरफ़ेस
- मुफ़्त और ओपन सोर्स (GPL-3.0)

## 📱 फ़र्मवेयर सूची

परखे गए फ़र्मवेयर की सूची — स्थिति, नोट्स और डाउनलोड लिंक के साथ — हमारे फ़ोरम पर है। वहाँ अपने नतीजे साझा करें, सवाल पूछें और नई इमेज खोजें।

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/%E0%A4%AB%E0%A4%BC%E0%A5%8B%E0%A4%B0%E0%A4%AE%20%E0%A4%96%E0%A5%8B%E0%A4%B2%E0%A5%87%E0%A4%82-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="फ़ोरम"/></a>

</div>

## 🚀 जल्दी शुरुआत

1. [Releases](https://github.com/uxazu/aemulator/releases) से APK डाउनलोड करके इंस्टॉल करें।
2. [फ़ोरम](https://aeforum.uxazuu.space/) पर फ़र्मवेयर खोजें और फ़ोन पर डाउनलोड करें।
3. AEmulator खोलें → **फ़र्मवेयर जोड़ें** और फ़ाइल चुनें। आयात में कुछ मिनट लगते हैं।
4. **चलाएँ** दबाएँ। पहला बूट धीमा होता है: सिस्टम ऐप्स ऑप्टिमाइज़ करता है।
5. ⋮ मेनू में वॉल्यूम, पावर बटन और लॉग; ⚙️ से सेटिंग्स और भाषा।

## 📋 आवश्यकताएँ

- 64-बिट ARM फ़ोन (arm64-v8a) पर Android 8.0+
- प्रति फ़र्मवेयर लगभग 1–3 GB खाली जगह
- नया Snapdragon / Dimensity / Tensor सुझाया जाता है

## ⚙️ यह कैसे काम करता है

हर गेस्ट प्रोसेस संशोधित यूज़र-मोड QEMU में चलता है। एक binder डेमन कर्नेल ड्राइवर की जगह लेता है, GL ब्रिज OpenGL ES कॉल GPU तक भेजता है, और छोटी गेस्ट लाइब्रेरी (ऑडियो HAL, audio policy रैपर, LD_PRELOAD शिम) निर्माताओं के कोड को एमुलेटर के अनुकूल बनाती हैं। इम्पोर्टर फ़र्मवेयर पढ़ता है, boot इमेज में init स्क्रिप्ट ढूँढता है और सेवाओं की शुरुआत की योजना बनाता है।

## 🛠️ सोर्स से बनाएँ

JDK 17, Android SDK 36 और NDK r28 चाहिए। गेस्ट लाइब्रेरी `native/*/build.sh` से बनती हैं।

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 आभार

AEmulator [मूल लेखक](https://t.me/istratii_tech) के HTC Desire HD और HTC One M7 एमुलेटर से विकसित हुआ — उनके इंजन के बिना यह प्रोजेक्ट नहीं होता।

## 💙 प्रोजेक्ट का समर्थन करें

अगर AEmulator ने आपका प्यारा फ़ोन लौटा दिया, तो आप विकास में मदद कर सकते हैं:

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💵 USDT (TRC20): `TN5cZFQ6BKPKCJZiUqEQKaifaEdtNUqaBF`
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 लिंक

- 🌐 वेबसाइट: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 फ़ोरम: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 Telegram चैनल: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 लेखक: [uxazu](https://github.com/uxazu)
- 🧬 मूल लेखक: [t.me/istratii_tech](https://t.me/istratii_tech)

## 📄 लाइसेंस

GPL-3.0। Android, ट्रेडमार्क और फ़र्मवेयर उनके स्वामियों के हैं।
