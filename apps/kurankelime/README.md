# Kur’an Kelime Atölyesi — Android 5 dil (v0.1.0 önizleme)

Bağımsız, internetsiz çalışan Kotlin / Jetpack Compose uygulaması.

## Özellikler

- Obsidian / Soft Harmony koyu ve gündüz teması.
- Arapça kelime altında 5 dilden biri: TR, EN, DE, ES, ZH.
- Kelimeye dokununca: favori, paylaş, çevir/dil seçimi, dinle.
- Ayet favorisi, devam edilen ayet/kelime, sure seçimi, çevrimdışı kelime araması.
- Veriler Android SQLite içinde saklanır; ağ izni yoktur.
- Ses yürütücü yalnız uygulama içine yerleştirilmiş, doğrulanmış kayıtları oynatır; hazır kayıt yoksa kullanıcıya bunu söyler.

## İÇERİK DURUMU — YAYIN ÖNCESİ

Bu, tam Kur'an uygulaması DEĞİLDİR. Pilot veri sadece Fâtiha suresindeki 7 ayettir; 5 dil kelime anlamları taslaktır ve ilmî kontrol bekler. Arapça kelime yazımları da bağımsız bir mushaf kaynağı ile doğrulanmalıdır. Kaynak PDF'deki hakları belirsiz Türkçe kelime karşılıkları kopyalanmamıştır.

Tam 114 sure, 6236 ayet ve tüm ses kayıtları ayrı veri/lisans çalışması tamamlandığında eklenebilir. Doğru kıraat kaydı yerine cihazın genel TTS motoru kullanılmaz. Uthmanic yazı tipi yerine şimdilik cihazın Arapça destekli serif yazı tipi kullanılır.

## Derleme

Depo kökünden: gradle --project-dir apps/kurankelime :app:assembleDebug :app:testDebugUnitTest

İmzalama yalnız özel altyapıda yapılmalıdır; public repoya keystore veya API anahtarı koymayın. Android 8.0+ (minSdk 26), Android 15 hedefi (targetSdk 35).

## Veri modeli

assets/seed/fatiha_preview.json -> cihazın SQLite veritabanı (sura/ayah/position, 5 dil, sözcük türü). Her kelime ayrı kimlik taşır. Ayet numaraları başlangıç ayetleriyle doğru eşleşir. İçerik ve fonetik doğrulama tamamlanmadan Play Store'a yayımlanmaz.
