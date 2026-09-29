<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**懐かしの Android ファームウェア — HTC Sense、TouchWiz、MIUI、AOSP — を最新スマホで。root も PC も不要。**

[![Version](https://img.shields.io/badge/version-0.0.0.2-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · [🇻🇳 Tiếng Việt](README.vi.md) · [🇨🇳 简体中文](README.zh-CN.md) · **🇯🇵 日本語** · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator はファームウェアファイルから本物の Android 2.3〜7.x を直接起動します：リカバリー ZIP、Odin アーカイブ、Google ファクトリーイメージ。古い ARM コードは改良版 QEMU で変換し、カーネルの binder をエミュレートし、描画はスマホの GPU、音声は Android のオーディオを使います。すべて普通のアプリの中で動きます。

## ✨ 特長

- ほぼすべての形式をインポート：CWM/TWRP ZIP、Samsung Odin `.tar.md5`、Google ファクトリー `.tgz`、`system.img`、OTA `system.new.dat.br`
- メーカー独自 UI がそのまま動作：HTC Sense、Samsung TouchWiz、MIUI、AOSP
- GL ブリッジによるハードウェア描画、音声、タッチとマルチタッチ、最新 TLS プロキシ付きネットワーク
- APK・音楽・写真用の共有メモリーカードフォルダー
- 18 言語対応の Material 3 Expressive UI
- 無料・オープンソース（GPL-3.0）

## 📱 ファームウェア一覧

動作確認済みファームウェアの一覧（状態・メモ・ダウンロードリンク付き）はフォーラムにあります。結果の共有や質問、新しいイメージ探しもそちらでどうぞ。

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/%E3%83%95%E3%82%A9%E3%83%BC%E3%83%A9%E3%83%A0%E3%82%92%E9%96%8B%E3%81%8F-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="フォーラム"/></a>

</div>

## 🚀 クイックスタート

1. [Releases](https://github.com/uxazu/aemulator/releases) から APK をダウンロードしてインストール。
2. [フォーラム](https://aeforum.uxazuu.space/)でファームウェアを探し、スマホにダウンロードします。
3. AEmulator を開き →**ファームウェアを追加**でファイルを選択。インポートには数分かかります。
4. **起動**をタップ。初回はアプリ最適化のため時間がかかります。
5. ⋮ メニューで音量・電源ボタン・ログ、⚙️ で設定と言語。

## 📋 動作要件

- 64 ビット ARM 端末（arm64-v8a）の Android 8.0+
- ファームウェア 1 つにつき約 1〜3 GB の空き
- 最新の Snapdragon / Dimensity / Tensor を推奨

## ⚙️ 仕組み

各ゲストプロセスは改良版ユーザーモード QEMU で動きます。binder デーモンがカーネルドライバーの代わりを務め、GL ブリッジが OpenGL ES 呼び出しを GPU に転送し、小さなゲストライブラリ（オーディオ HAL、audio policy ラッパー、LD_PRELOAD シム）がメーカーのコードをエミュレーターに合わせます。インポーターはファームウェアを読み、boot イメージの init スクリプトからサービスの起動計画を作ります。

## 🛠️ ソースからビルド

JDK 17、Android SDK 36、NDK r28 が必要です。ゲスト用ライブラリは `native/*/build.sh` でビルドします。

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 クレジット

AEmulator は[オリジナル作者](https://t.me/istratii_tech)の HTC Desire HD・HTC One M7 エミュレーターから生まれました。そのエンジンなしにこのプロジェクトはありません。

## 💙 プロジェクトを支援

AEmulator で思い出のスマホがよみがえったら、開発を支援できます：

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💵 USDT (TRC20): `TN5cZFQ6BKPKCJZiUqEQKaifaEdtNUqaBF`
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 リンク

- 🌐 ウェブサイト: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 フォーラム: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 Telegram チャンネル: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 作者: [uxazu](https://github.com/uxazu)
- 🧬 オリジナル作者: [t.me/istratii_tech](https://t.me/istratii_tech)

## 📄 ライセンス

GPL-3.0。Android、商標、ファームウェアは各所有者に帰属します。
