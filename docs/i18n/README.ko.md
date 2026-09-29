<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**클래식 Android 펌웨어 — HTC Sense, TouchWiz, MIUI, AOSP — 를 최신 휴대폰에서. 루팅도 PC도 필요 없습니다.**

[![Version](https://img.shields.io/badge/version-0.0.0.2-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · **🇰🇷 한국어**

</div>

---

AEmulator는 펌웨어 파일에서 진짜 Android 2.3–7.x 시스템을 바로 부팅합니다: 리커버리 ZIP, Odin 아카이브, Google 팩토리 이미지. 오래된 ARM 코드는 수정된 QEMU가 변환하고, 커널 binder를 에뮬레이션하며, 그래픽은 휴대폰 GPU로, 소리는 Android 오디오로 처리합니다. 모두 평범한 앱 안에서 동작합니다.

## ✨ 기능

- 거의 모든 형식 가져오기: CWM/TWRP ZIP, 삼성 Odin `.tar.md5`, Google 팩토리 `.tgz`, `system.img`, OTA `system.new.dat.br`
- 제조사 UI가 그대로 동작: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- GL 브리지 하드웨어 그래픽, 소리, 터치와 멀티터치, 최신 TLS 프록시 네트워크
- APK·음악·사진을 위한 공유 메모리 카드 폴더
- 18개 언어의 Material 3 Expressive 인터페이스
- 무료 오픈 소스(GPL-3.0)

## 📱 펌웨어 목록

검증된 펌웨어 목록(상태, 메모, 다운로드 링크 포함)은 포럼에 있습니다. 결과를 공유하고 질문하고 새 이미지를 찾아보세요.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/%ED%8F%AC%EB%9F%BC%20%EC%97%B4%EA%B8%B0-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="포럼"/></a>

</div>

## 🚀 빠른 시작

1. [Releases](https://github.com/uxazu/aemulator/releases)에서 APK를 받아 설치합니다.
2. [포럼](https://aeforum.uxazuu.space/)에서 펌웨어를 찾아 휴대폰에 내려받으세요.
3. AEmulator → **펌웨어 추가**에서 파일을 고릅니다. 가져오기는 몇 분 걸립니다.
4. **시작**을 누릅니다. 첫 부팅은 앱 최적화로 더 오래 걸립니다.
5. ⋮ 메뉴에서 볼륨·전원 버튼·로그, ⚙️에서 설정과 언어.

## 📋 요구 사항

- 64비트 ARM 휴대폰(arm64-v8a)의 Android 8.0+
- 펌웨어당 약 1–3GB 여유 공간
- 최신 Snapdragon / Dimensity / Tensor 권장

## ⚙️ 작동 방식

각 게스트 프로세스는 수정된 사용자 모드 QEMU에서 실행됩니다. binder 데몬이 커널 드라이버를 대신하고, GL 브리지가 OpenGL ES 호출을 GPU로 전달하며, 작은 게스트 라이브러리(오디오 HAL, audio policy 래퍼, LD_PRELOAD 심)가 제조사 코드를 에뮬레이터에 맞춥니다. 가져오기 도구는 펌웨어를 읽고 boot 이미지의 init 스크립트로 서비스 시작 계획을 만듭니다.

## 🛠️ 소스에서 빌드

JDK 17, Android SDK 36, NDK r28이 필요합니다. 게스트 라이브러리는 `native/*/build.sh`로 빌드합니다.

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 감사의 말

AEmulator는 [원작자](https://t.me/istratii_tech)의 HTC Desire HD·HTC One M7 에뮬레이터에서 시작되었습니다. 그의 엔진이 없었다면 이 프로젝트도 없었습니다.

## 💙 프로젝트 후원

AEmulator로 추억의 휴대폰을 되살렸다면 개발을 후원할 수 있습니다:

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💵 USDT (TRC20): `TN5cZFQ6BKPKCJZiUqEQKaifaEdtNUqaBF`
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 링크

- 🌐 웹사이트: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 포럼: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 텔레그램 채널: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 제작자: [uxazu](https://github.com/uxazu)
- 🧬 원작자: [t.me/istratii_tech](https://t.me/istratii_tech)

## 📄 라이선스

GPL-3.0. Android, 상표, 펌웨어는 각 소유자에게 있습니다.
