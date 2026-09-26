<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**Les anciennes ROM Android — HTC Sense, TouchWiz, MIUI, AOSP — sur un téléphone moderne. Sans root, sans PC.**

[![Version](https://img.shields.io/badge/version-0.0.0.1-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · **🇫🇷 Français** · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator démarre un vrai système Android 2.3–4.4 directement depuis un fichier de ROM : ZIP recovery, archive Odin ou image d’usine Google. L’ancien code ARM passe par un QEMU modifié, le binder du noyau est émulé, les graphismes utilisent le GPU du téléphone et le son passe par la pile audio d’Android — le tout dans une application ordinaire.

## ✨ Fonctionnalités

- Import de presque tous les formats : ZIP CWM/TWRP, Odin `.tar.md5` de Samsung, image d’usine Google `.tgz`, `system.img`, OTA `system.new.dat.br`
- Les surcouches constructeur fonctionnent telles quelles : HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Graphismes matériels via le pont GL, son, tactile et multitouch, réseau avec proxy TLS moderne
- Dossier de carte mémoire partagé pour les APK, la musique et les photos
- Interface Material 3 Expressive en 18 langues
- Gratuit et open source (GPL-3.0)

## 📱 Liste des ROM

La liste des ROM vérifiées — avec états, notes et liens de téléchargement — se trouve sur notre forum. Partagez-y vos résultats, posez vos questions et trouvez de nouvelles images.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/Ouvrir%20le%20forum-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="Forum"/></a>

</div>

## 🚀 Démarrage rapide

1. Téléchargez l’APK depuis [Releases](https://github.com/uxazu/aemulator/releases) et installez-le.
2. Trouvez une ROM sur le [forum](https://aeforum.uxazuu.space/) et téléchargez-la sur le téléphone.
3. Ouvrez AEmulator → **Ajouter une ROM** et choisissez le fichier. L’import prend quelques minutes.
4. Appuyez sur **Démarrer**. Le premier démarrage est plus long : le système optimise les applis.
5. Menu ⋮ pour le volume, le bouton marche et le journal ; ⚙️ ouvre les paramètres et la langue.

## 📋 Configuration requise

- Android 8.0+ sur un téléphone ARM 64 bits (arm64-v8a)
- Environ 1–3 Go libres par ROM
- Puce récente conseillée : Snapdragon / Dimensity / Tensor

## ⚙️ Fonctionnement

Chaque processus invité tourne sous un QEMU en mode utilisateur modifié. Un démon binder remplace le pilote du noyau, un pont GL transmet les appels OpenGL ES au GPU, et de petites bibliothèques invitées (HAL audio, enveloppe audio policy, shim LD_PRELOAD) adaptent le code constructeur à l’émulateur. L’importateur lit la ROM, trouve les scripts init dans l’image boot et construit le plan de démarrage des services.

## 🛠️ Compiler depuis les sources

Il faut JDK 17, Android SDK 36 et NDK r28. Les bibliothèques invitées se compilent avec `native/*/build.sh`.

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 Remerciements

AEmulator est né des émulateurs HTC Desire HD et HTC One M7 de [l’auteur d’origine](https://t.me/istratii_tech) — sans son moteur, ce projet n’existerait pas.

## 💙 Soutenir le projet

Si AEmulator vous a rendu un téléphone que vous aimiez, vous pouvez soutenir le développement :

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💵 USDT (TRC20): `TN5cZFQ6BKPKCJZiUqEQKaifaEdtNUqaBF`
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 Liens

- 🌐 Site web: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 Forum: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 Chaîne Telegram: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 Auteur: [uxazu](https://github.com/uxazu)
- 🧬 Auteur d’origine: [t.me/istratii_tech](https://t.me/istratii_tech)

## 📄 Licence

GPL-3.0. Android, les marques et les ROM appartiennent à leurs propriétaires.
