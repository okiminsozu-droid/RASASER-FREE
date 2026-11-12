# Kur’anı Keşfet v0.3.3 — Özel, tam içerikli Android TEST APK

**Açık kaynak PR ve GitHub Actions yalnız uygulama kodunu derler.** 605 sayfalık özel görsel paketi, kaynak PDF, OCR seçim veritabanları ve imza anahtarları açık GitHub deposuna **yüklenmez**. Bu nedenle GitHub'daki genel kod-önizleme APK'si bütün sayfaları içermez.

## Tam APK'yi yerel bilgisayarda güvenle üretme

Gerekenler: Java 17, Android SDK platform 35, Gradle 8.9, bu deponun `apps/kurankelime` klasörü ve kullanıcıya ait şu doğrulanmış özel dosyalar:

- `Kurani_Kesfet_605_Tekli_Sayfa_Kayipsiz_Gorsel_Paketi.zip` (605 WebP, `pages.json`, isteğe bağlı `page_labels.json`).
- `quran_605_selectable_text_layer.sqlite` (605 ekran kelime koordinatları, özgün PDF SHA-256'sı).

Depo kökünden:

```bash
python3 apps/kurankelime/tools/build_private_apk.py \
  --pages /guvenli/konum/Kurani_Kesfet_605_Tekli_Sayfa_Kayipsiz_Gorsel_Paketi.zip \
  --selection /guvenli/konum/quran_605_selectable_text_layer.sqlite \
  --output /guvenli/konum/Kurani_Kesfet_v0.3.3_TAM_TEST.apk
```

İsteğe bağlı `--ocr quran_304_offline_ocr.sqlite` ve `--logo logo.webp` parametreleri desteklenir. **Dosyalar internete yüklenmez.** Derleme, geçici ayrı bir proje kopyasında yapılır. 605 sayfanın hepsi, `pages.json` eşleştirmeleri, WebP başlıkları, ZIP kayma saldırısı, veritabanı `quick_check`, sayfa sayısı, kelime sayısı ve özgün PDF kaynak kimliği denetlenir. Son APK açılarak içeriklerin gerçekten gömülü olduğu tekrar kontrol edilir.

Oluşan `offlineTest` sürümünün paket son eki `.trial` ve Android debug TEST imzasıdır. **Eski ana kurulumun üzerine yazmaz; eski favorileri ve kayıtları değiştirmez.** Aynı paket adıyla uygulama güncellemesi ancak aynı imza anahtarıyla yapılabilir; üretim keystore'u bu araç tarafından kullanılmaz veya yüklenmez.

### Yayın öncesi zorunlu kontroller

- Kaynak görsellerin yayım/telif izinleri ve kelime anlamlarının ilmî doğruluğu.
- 605 sayfada sayfa üstü, altı, kelime dokunma, kelime aralığı seçimi, kopyalama, Wi-Fi ile ilk çeviri modeli indirme ve sonraki uçak-modu testi.
- Google ML Kit otomatik çevirilerinin **doğrulanmış Kur’an meali sayılmaması**; gerçek onaylı sözlük olmadan böyle etiketlenmemesi.
- Eski stabil APK ve imza anahtarı yedeklerinin korunması.

Bu doküman kodu/yerel birleştirme yolunu tamamlar; **özel sayfa ZIP'i ve SQLite dosyası olmadan eksiksiz yeni APK üretildiği iddia edilmez.**
