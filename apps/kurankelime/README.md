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

## Onaylanan PDF'nin sayfa sayfa görüntülenmesi

PDF'nin birebir korunması için uygulama Android PdfRenderer kullanır. Eserin 304 sayfası aynı sıra ve renklerle görüntülenir; PDF yeniden dizilmez, Arapça/Türkçe yazılar değiştirilmez.

Kaynak PDF: `Renkli_Kelime_Altli_Kuran_TAMAMI_Calisma_Nushasi.pdf` (90.889.211 bayt; SHA-256 `387674c9a3bfb2c501c2dcca4b63946ea05f386dcd4d6fa4b63b4060b01397ea`).

**Android kaynak klasörü:** `app/src/main/assets/quran/kelime_mealli.pdf`. Küçük onaylı logo: `app/src/main/assets/brand/logo.webp`. Bu büyük PDF ve telif durumu incelenen tercüme, herkese açık GitHub depomuzun kaynaklarına konmamıştır; bağımsız ZIP içerik paketinden bu yola yerleştirilince uygulama tamamen çevrimdışı ön yüklü çalışır. PDF yoksa Android dosya seçicisinden aynı dosya bir kez içeri alınır ve cihazda saklanır. İçerik aktarma kontrolü dosyanın tam SHA-256 karmasını ve 304 sayfa sayısını doğrular.

Okuma: sol ok sonraki PDF sayfası, sağ ok önceki PDF sayfası, üstte ana sayfa, sayfa numarası, favori ve logolu PNG paylaşım. PDF 304 sayfalıdır; klasik 604 mushaf sayfası ile aynı numaralandırma değildir. İlk PDF sayfasındaki Fâtiha ayetlerine sınırlı arama/vurgulama vardır; **tüm 304 sayfada kelime arama henüz çalışmıyor** çünkü metinler PDF'de resim katmanı olarak duruyor. Farklı dilde UI değişebilir ama bu PDF'nin içindeki kelime altları Türkçedir. Kaynak PDF'nin tercüme hakları ve metin doğruluğu kamuya yayın öncesi ayrıca kontrol edilmelidir.
