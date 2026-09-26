<div align="center">

<img src="../../docs/assets/logo.png" width="128" alt="AEmulator logo"/>

# AEmulator

**Firmware Android cổ điển — HTC Sense, TouchWiz, MIUI, AOSP — trên điện thoại hiện đại. Không root, không cần PC.**

[![Version](https://img.shields.io/badge/version-0.0.0.1-3D5AFE?style=for-the-badge)](https://github.com/uxazu/aemulator/releases) [![License](https://img.shields.io/badge/license-GPL--3.0-3D5AFE?style=for-the-badge)](../../LICENSE) [![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/uxazu/aemulator) [![Forum](https://img.shields.io/badge/Forum-aeforum-FF6D00?style=for-the-badge&logo=discourse&logoColor=white)](https://aeforum.uxazuu.space/) [![Telegram](https://img.shields.io/badge/Telegram-channel-26A5E4?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/aemulatorofficial) [![Website](https://img.shields.io/badge/site-aemulator.gt.tc-111?style=for-the-badge)](https://aemulator.gt.tc)

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](README.ru.md) · [🇺🇦 Українська](README.uk.md) · [🇩🇪 Deutsch](README.de.md) · [🇫🇷 Français](README.fr.md) · [🇪🇸 Español](README.es.md) · [🇧🇷 Português](README.pt-BR.md) · [🇮🇹 Italiano](README.it.md) · [🇵🇱 Polski](README.pl.md) · [🇹🇷 Türkçe](README.tr.md) · [🇸🇦 العربية](README.ar.md) · [🇮🇷 فارسی](README.fa.md) · [🇮🇳 हिन्दी](README.hi.md) · [🇮🇩 Indonesia](README.id.md) · **🇻🇳 Tiếng Việt** · [🇨🇳 简体中文](README.zh-CN.md) · [🇯🇵 日本語](README.ja.md) · [🇰🇷 한국어](README.ko.md)

</div>

---

AEmulator khởi động một hệ thống Android 2.3–4.4 thật trực tiếp từ tệp firmware: ZIP recovery, gói Odin hoặc factory image của Google. Mã ARM cũ được dịch bằng QEMU đã chỉnh sửa, binder của nhân được giả lập, đồ họa dùng GPU điện thoại và âm thanh đi qua hệ thống âm thanh Android — tất cả trong một ứng dụng bình thường.

## ✨ Tính năng

- Nhập gần như mọi định dạng: ZIP CWM/TWRP, Odin `.tar.md5` của Samsung, factory `.tgz` của Google, `system.img`, OTA `system.new.dat.br`
- Giao diện hãng chạy nguyên bản: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Đồ họa phần cứng qua cầu GL, âm thanh, cảm ứng và đa điểm, mạng với proxy TLS hiện đại
- Thư mục thẻ nhớ dùng chung cho APK, nhạc và ảnh
- Giao diện Material 3 Expressive với 18 ngôn ngữ
- Miễn phí và mã nguồn mở (GPL-3.0)

## 📱 Danh sách firmware

Danh sách firmware đã kiểm tra — kèm trạng thái, ghi chú và liên kết tải — nằm trên diễn đàn của chúng tôi. Tại đó bạn có thể chia sẻ kết quả, đặt câu hỏi và tìm image mới.

<div align="center">

<a href="https://aeforum.uxazuu.space/"><img src="https://img.shields.io/badge/M%E1%BB%9F%20di%E1%BB%85n%20%C4%91%C3%A0n-aeforum.uxazuu.space-FF6D00?style=for-the-badge&logo=discourse&logoColor=white" height="44" alt="Diễn đàn"/></a>

</div>

## 🚀 Bắt đầu nhanh

1. Tải APK từ [Releases](https://github.com/uxazu/aemulator/releases) và cài đặt.
2. Tìm firmware trên [diễn đàn](https://aeforum.uxazuu.space/) và tải về điện thoại.
3. Mở AEmulator → **Thêm firmware** và chọn tệp. Việc nhập mất vài phút.
4. Nhấn **Chạy**. Lần khởi động đầu chậm hơn: hệ thống tối ưu ứng dụng.
5. Menu ⋮ để chỉnh âm lượng, nút nguồn và nhật ký; ⚙️ mở cài đặt và ngôn ngữ.

## 📋 Yêu cầu

- Android 8.0+ trên điện thoại ARM 64-bit (arm64-v8a)
- Khoảng 1–3 GB trống cho mỗi firmware
- Nên dùng Snapdragon / Dimensity / Tensor đời mới

## ⚙️ Cách hoạt động

Mỗi tiến trình khách chạy dưới QEMU chế độ người dùng đã chỉnh sửa. Một daemon binder thay cho driver nhân, cầu GL chuyển lệnh OpenGL ES tới GPU và các thư viện khách nhỏ (HAL âm thanh, lớp bọc audio policy, shim LD_PRELOAD) điều chỉnh mã của hãng cho hợp với trình giả lập. Trình nhập đọc firmware, tìm script init trong image boot và lập kế hoạch khởi động dịch vụ.

## 🛠️ Biên dịch từ mã nguồn

Cần JDK 17, Android SDK 36 và NDK r28. Thư viện khách được dựng bằng `native/*/build.sh`.

```bash
git clone https://github.com/uxazu/aemulator.git
cd aemulator
./gradlew assembleRelease
```

## 🙏 Ghi công

AEmulator phát triển từ trình giả lập HTC Desire HD và HTC One M7 của [tác giả gốc](https://t.me/istratii_tech) — không có engine của anh ấy thì không có dự án này.

## 💙 Ủng hộ dự án

Nếu AEmulator mang lại chiếc điện thoại bạn yêu thích, bạn có thể ủng hộ phát triển:

- 💳 [dalink.to/uxazu](https://dalink.to/uxazu)
- 💵 USDT (TRC20): `TN5cZFQ6BKPKCJZiUqEQKaifaEdtNUqaBF`
- 💎 TON: `UQCDtAs_DWUUKStpnHBOBo72VA7C044PPo1asfpq6vQHAF-V`

## 🔗 Liên kết

- 🌐 Trang web: [aemulator.gt.tc](https://aemulator.gt.tc)
- 💬 Diễn đàn: [aeforum.uxazuu.space](https://aeforum.uxazuu.space/)
- 📣 Kênh Telegram: [@aemulatorofficial](https://t.me/aemulatorofficial)
- 👤 Tác giả: [uxazu](https://github.com/uxazu)
- 🧬 Tác giả gốc: [t.me/istratii_tech](https://t.me/istratii_tech)

## 📄 Giấy phép

GPL-3.0. Android, nhãn hiệu và firmware thuộc về chủ sở hữu.
