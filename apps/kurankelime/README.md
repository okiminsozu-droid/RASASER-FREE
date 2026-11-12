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


## 11 Kasım 2026 — Kur’anı Keşfet v0.2.0 deneme düzeltmeleri

- Beyaz/kum renkli ana ekran korunmuştur. Türkçe ve İngilizce dekoratif slogan, RaSaSeR marka işareti, ücretsiz kullanım açıklamaları ve `www.okiminsozu.com` eklenmiştir.
- PDF sayfa okuma alanı mevcut ekrana **tam sayfa sığdırma** ile yerleştirilmiştir. 100% görünümde sol parmak kaydırması sonraki, sağ parmak kaydırması önceki sayfaya geçer. 150–300% büyütmede içerik iki yönde kaydırılır.
- Arama yalnız Fâtiha örneği olmaktan çıkmıştır. **Tam test APK** içine ayrıca yerleştirilen `app/src/main/assets/quran/word_positions.tsv.gz` dosyası, kaynak PDF'nin mevcut Türkçe kelime anlamlarının metin katmanından çıkarılmış **86.973 konumlu anlam satırını** içerir. Bu kaynak veri *herkese açık depoya konulmamıştır*. PDF görselleri değişmemiştir.
- Arama sonuçları kelime karşılığı ve tam PDF sayfa numarası ile gösterilir; satıra basılınca sayfaya gidilir ve eşleşen anlamın bulunduğu bölge altın renkli işaretlenir. PDF sayfasında kelimenin/altındaki Türkçe karşılığın üzerine dokunulunca konumlu vurgulama yapılır.
- Telif hakları henüz doğrulanmamış kelime karşılığı verileri ve PDF kamuya açık kaynak/Actions çıktılarında **bulunmaz**. GitHub'dan yalnız kaynak uygulaması derlendiğinde Fâtiha örnek araması mevcuttur. Tam arama için özel, çevrimdışı içerik paketi gerekir.
- Yeni deneme APK'sinin `debug.applicationIdSuffix=".trial"` değeri vardır. Böylece önceki test APK'si ve onun favorileri silinmeden **yan yana kurulabilir**. Favorilerin otomatik aktarılması yapılmamıştır.
- Kaynak Android derlemesi GitHub Actions ile doğrulanmış; özel test APK'sinin PDF (SHA-256 `387674c9a3bfb2c501c2dcca4b63946ea05f386dcd4d6fa4b63b4060b01397ea`) ve indeks bütünlüğü ayrı kontrol edilmiştir. Cihaz üzerinde uçtan uca test için kullanıcı incelemesi gereklidir.
- Kamuya açık dağıtımdan önce PDF ve kelime karşılıkları için telif izni, ayet eşleştirme ve 5 dil içeriği uzman kontrolü gerekir. PDF'nin içindeki anlamlar yalnız Türkçedir; ana ekran dili değişse de PDF metni otomatik çevrilmez.


## 11 Kasım 2026 — v0.2.1: çevrimdışı metin motoru ve dokunma onarımı

- PDF okuma etkinliğinde Android sistem çubukları gizlenir. Gezinme araçları sayfa alanını daraltmak yerine üstte/ altta sayfanın üstüne çizilir; tam sayfa orantılı sığdırılır, metin bozulmaz. Çift dokunuş araçları açıp kapatır; büyütmede iki yönlü kaydırma çalışır.
- %100'de sola kaydırma **sonraki**, sağa kaydırma **önceki** sayfa; büyütmede sayfa değiştirmek yerine içeriği kaydırır.
- PDF üzerinde tek dokunuş veya uzun basma, Android 15+ sistem PDF metin seçicisini kullanır; seçili metin vurgulanıp kopyalanabilir. Metin katmanı bulunmadığında uydurma sözcük gösterilmez.
- Yerleşik `PdfTextEngine.kt`, Android 15+ üzerinde **uygulama içindeki veya bir kez cihazdan alınmış gerçek PDF'nin metin katmanını** çevrimdışı tarar ve cihazın kendi SQLite FTS4 veritabanına konumlarıyla indeksler. Tarama kaldığı sayfadan devam edebilir. Arama sonuçlarından ilgili PDF sayfasına gidilir.
- Android 8–14 için, lisansı doğrulanmış tam kelime dizini ayrıca uygulama içinde varsa eski yerel dizin kullanılır; bulunmuyorsa yalnız Fâtiha önizleme araması kalır. Android 15+ üzerinde PDF sadece taranmış görüntüden oluşuyorsa **OCR yapılmaz** ve tam metin araması vaat edilmez.
- Bu açık kaynak depo 90 MB PDF'yi veya çevirisinin hakları belirsiz metinlerini içermez. Yerel metin motoru PDF'yi cihaz içinde işler ve internete göndermez. İlk tarama tamamlanmadan tüm sayfaları kapsayan arama sonuçları gösterilmez.
- Sürüm `0.2.1` (versionCode 3). Bu bir test önizlemesidir; Android 15 cihazda gerçek PDF ile uçtan uca doğrulama ve içerik/lisans kontrolü tamamlanmadan yayımlanmaz.


## 12 Kasım 2026 — v0.3.3: Seç / Kopyala / Çevir (korumalı)

- `StableReaderActivity` üzerindeki **bir dokunuşla seçim** ve sürüklenebilir OCR işaretçileri korunur. Seçim çubuğunda artık erişilebilir Kopyala/Çevir **simgeleri** bulunur; sayfadan ayrılınmaz.
- Çeviri, `OnDeviceWordTranslator` ile aynı okuma ekranında küçük Android penceresinde gösterilir. Hedef dil ana sayfadaki `discovery_home/lang` tercihidir (tr/en/de/es/zh/ar). Mevcut arama, sayfa sayacı, favoriler, paylaşma ve orijinal 605 ekran görüntüsü değiştirilmez.
- İlk kez gerekli ML Kit modeli gerekiyorsa kullanıcıdan **Wi-Fi ile model indirme izni** alınır. Model cihazda tutulduğu sürece sonraki çeviri çevrimdışı çalışır; yalnız model indirmek internet gerektirir. Ücretli Google Cloud Translation API **kullanılmaz**. Çeviri sonucu daima **otomatik** olarak etiketlenir ve tefsir/meal yerine geçmez.
- **Doğrulanmış anlam varsa önce o** görünür; otomatik çeviri isteğe bağlı ayrıca görülebilir. Bu kalite kapısı sadece uygulama içine özel olarak yerleştirilebilecek ve bağımsız incelemesi yapılmış `assets/quran/verified_word_glosses.sqlite` veritabanı için aktiftir. **Kamuya açık depo bu veritabanını içermez; mevcut OCR kelimeleri doğrulanmış anlam sayılmaz.**
- İsteğe bağlı veritabanı şeması: `reviewed_word_glosses(page INTEGER, ocr_id INTEGER, source_language TEXT, target_language TEXT, source_text TEXT, meaning TEXT, source_reference TEXT, reviewer TEXT, review_status TEXT, source_pdf_sha256 TEXT)`. Sadece `review_status='approved'`, boş olmayan kaynak/hakem, özgün PDF SHA-256 eşleşmesi, tek OCR kimliği ve kaynak kelimenin birebir eşleşmesi olan kayıt kullanılır. Çok kelimeli seçimlerde doğrulanmış tek kelime anlamı uydurulmaz.
- Önceki test paketleri/projeler değişmez; bu kod yalnız ayrı geliştirme dalındadır. Eski kurulu APK, farklı imzalı bir APK ile üstüne yüklenemez. İçerik paketi açık kaynakta paylaşılmaz.
