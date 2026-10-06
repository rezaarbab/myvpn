# MyVPN

کلاینت VPN اندرویدی سبک با هسته‌ی [sing-box](https://github.com/SagerNet/sing-box) — Kotlin + Jetpack Compose + Material 3.

## معماری

```
Kotlin
 └── Jetpack Compose (UI)
       └── Material 3 (design system)
             └── libbox AAR (هسته‌ی sing-box، در CI بیلد می‌شود)
```

- **هسته**: `sing-box v1.14.2` با تگ‌های بیلد سمت کلاینت (`with_gvisor, with_quic, with_utls, with_wireguard, with_clash_api, with_ech`) — بدون هیچ قابلیت کلاینتی از دست رفته، ولی بدون حجم اضافه‌ی سمت سرور.
- **CI**: یک workflow واحد، دو جاب: (۱) بیلد `libbox.aar` با gomobile، (۲) بیلد APK امضاشده و آپلود آرتیفکت. روی تگ `v*` ریلیز خودکار ساخته می‌شود.
- **پروتکل‌ها**: VLESS (+Reality)، VMess، Trojan، Shadowsocks، Hysteria2، TUIC، WireGuard — هم با لینک اشتراکی هم دستی، هم کانفیگ JSON خام.

## استفاده

1. از تب **Actions** آخرین بیلد `Build` را باز کنید و آرتیفکت `app-release-apk` را دانلود کنید (نیاز به ورود به GitHub دارید).
2. در اپ، صفحه‌ی **سرورها → +** یک لینک `vless://…` بچسبانید یا دستی پر کنید.
3. سرور را انتخاب کنید و دکمه‌ی اتصال را بزنید.

## امضا (keystore)

اگر secret های `SIGNING_KEYSTORE_B64` (base64 فایل keystore)، `SIGNING_STORE_PASSWORD`، `SIGNING_KEY_ALIAS`، `SIGNING_KEY_PASSWORD` را تنظیم کنید، APK با همان امضا ساخته می‌شود. در غیر این صورت هر بیلد یک keystore موقت می‌سازد و **فایل آن را به‌عنوان آرتیفکت `signing-keystore` آپلود می‌کند** — آن را نگه دارید و بعداً به‌صورت secret اضافه کنید تا امضای اپ ثابت بماند (وگرنه برای هر به‌روزرسانی باید اپ را حذف/نصب مجدد کرد).

## نکته‌ی مجوز (GPL-3.0)

هسته‌ی sing-box تحت GPL-3.0-or-later منتشر می‌شود. این ریپو سورس بیلد هسته (تگ‌ها و workflow) را عمومی نگه می‌دارد. ضمناً طبق بند اضافه‌ی لایسنس sing-box، از نام آن در محصول استفاده نشده است.

## نقشه‌ی راه

- **فاز ۱ (همین نسخه)**: اپ + هسته + افزودن سرور دستی/لینک ✓
- **فاز ۲**: گرفتن تنظیمات/سرور از Cloudflare Workers (سابسکریپشن per-user با توکن قابل ابطال) + لایه‌ی رمزگذاری سفارشی.
