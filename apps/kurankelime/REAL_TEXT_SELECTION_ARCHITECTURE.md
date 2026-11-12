# Kur’anı Keşfet — gerçek metin seçimi / PDF Extra benzeri çevrimdışı motor

**Durum:** teknik araştırma + mimari karar (uygulamaya henüz geçirilmiş / telefonda doğrulanmış değildir). Mevcut çalışan 605 sayfa, arama, favoriler, imzalama ve marka görselleri KORUNUR.

## 1. Gerçek kaynak denetimi (2026-11-12)

Kullanıcının verdiği 90.889.211 baytlık `Renkli_Kelime_Altli_Kuran_TAMAMI_Calisma_Nushasi.pdf`, PyMuPDF ile **304/304 sayfa** tarandı.

- 304/304 kaynak sayfada raster içerik var; PDF içinde **1.208 görüntü bloğu** bulundu.
- Makine tarafından seçilebilir mevcut PDF sözcükleri **4.871** adet; sayfa başına **15–20**, medyan **16**. Bunlar kaynak başlığı/sayfa numarası gibi bilgilerdir, Kur’an gövde kelimeleri değildir.
- 605 tekli WebP sayfa, içerik gösterimidir, metin nesnesi değildir.
- OCR kaynaklı `quran_605_selectable_text_layer.sqlite`: **195.379 aday** (71.816 ara + 123.563 tur); doğrulanmış kelime/cümle kapsamı olduğu anlamına GELMEZ.

Sonuç: Görüntüyü yakınlaştırıp en yakın OCR kutusunu seçmek, gerçek metin seçimini sağlamaz. Tahmini OCR hatası olan kelimeler veya bir cümlenin çok kelimeli sırası bu yöntemle doğru kopyalanamaz.

## 2. PDF Extra / MobiPDF hakkında doğrulanabilenler

Resmî MobiPDF Android OCR rehberi, OCR ile taranmış dokümana **görünmez, makine tarafından okunabilir metin katmanı** koyduğunu; bu sayede arama, işaretleme, kopyalama ve seçim yapılabildiğini söylüyor.

Bu kapalı kaynak ürünün iç OCR motoru, seçimin tüm cihazlarda birebir algoritması, çevrimdışı çeviri sağlayıcısı resmî açıklamada teşhis edilmemektedir. Aynı işlevleri tasarlıyoruz; uygulamanın kodunu/markasını kopyalamıyoruz.

Kaynak: https://support.mobisystems.com/hc/en-us/articles/25129178070941-How-to-use-OCR-with-MobiPDF-on-Android

## 3. Bağımsız motor: iki belge yolu

**YOL A — Doğuştan metinli PDF** (Word'den dönüştürülmüş, gömülü Unicode metin taşıyan):
1. `PdfDocumentInspector`: sayfa başına metin içeriklerini ve karakter bounding box'larını değerlendir.
2. Android 15'te `PdfRenderer.Page.getTextContents()`, `selectContent(SelectionBoundary,SelectionBoundary)`; uygun cihazlarda Jetpack `PdfViewerFragment` ile gerçek seçimi kullan. Android 28+ uyumluluk ve SDK extension denetimi yap.
3. Raster OCR çalıştırma! Doğrudan Unicode kopyala.
4. Görünüm modları: Fit Page (tam sayfa), Fit Width (dikey kaydırma), Zoom + Pan; PDF'nin sayfa boyu/oranı korunur.

**YOL B — Görüntü PDF / taranmış belge** (şu anki Kur’an kaynağı):
1. Sayfayı orijinal görüntüsüyle ekrana koy, kayıtlı düşük çözünürlüklü görseli tekrar tekrar ölçekleyip kalite uydurma.
2. Arapça için uygun offline OCR (örn. Apache-2.0 Tesseract `ara`), Türkçe için `tur`, yerel ayrı bloklar/çizgiler/harfler. ML Kit Text Recognition v2 Arapça yazı sistemini desteklemiyor.
3. OCR'ı bir defa ön işleme sırasında (bizim sabit 605 sayfamızda) yap, sonra kalite kontrol ve kanonik Arapça metinle hizala. Diğer kullanıcı PDF'lerinde OCR cihazda veya kullanıcının açık seçimiyle yapılabilir.
4. Her satır/kelime için `page_id, block_id, line_id, ordinal, unicode, bbox, baseline, direction, status` tut. Salt `x,y,w,h` ve özgüven puanı yeterli değildir.
5. Kullanıcı uzun basınca en yakın **doğrulanmış kelime ve karakter sınırı** bulunur; iki seçim tutamacı sürüklenince kelime/cümle aralığı işaretlenir.
6. Görünmez metin katmanı / PDF text layer veya uygulama-içi yapılandırılmış `TextSelectionOverlay` ile gerçekte kopyalanan metin doğru Unicode sırasından gelir. PDF'ye yazılacak gizli metin için Arapça Unicode, RTL, hareke, ToUnicode ve metin çıkarma ayrı test edilmelidir.
7. OCR doğrulanmamışsa sessizce yanlış kelimeyi KUR’AN metni olarak kopyalama; güvenilirlik uyarısını sadece bilgi ekranında göster.

## 4. Kur’an için veri doğrulama

- Sure/ayet/kelime kimliği ve standart Arapça metin *kaynağı/lisansı doğrulanmış* bir veri setinden, OCR'den bağımsız olarak alınmalı.
- Arapça yazım, hareke, ayet sembolleri korunmalı; ekrandaki görsel kelimeyi kanonik dizideki kelimeye eşleştirmek için RTL satır yerleşimi ve sıra denetlenmeli.
- Türkçe **kelime karşılıkları** mevcut lisanslı/doğrulanmış kullanıcı içeriğinden kontrol edilmeli; OCR tahmini veya genel makine çevirisi "kesin kelime meali" diye sunulmamalı.
- Çok kelimeli seçimde RTL görsel sırasıyla mantıksal Unicode okuma sırası ayrı tutulmalı. Kopyala tam metin; Çevir seçilen aralık.
- Kullanıcının gönderdiği referansın telif/atıf şartları ve üçüncü taraf korpusların lisansı ayrıca incelenmeli.

Quran Foundation Content Sync, biçim ve kelime verisi sağlayabiliyor ama Developer Terms (2026-10-11) API içeriğinin çevrimdışı paketlenmesi/senkronizasyonu konusunda şartlar koyuyor; veri paketini izinsiz doğrudan APK'ye gömmeyin.
https://api-docs.quran.com/legal/developer-terms/
https://api-docs.quran.com/docs/tutorials/content-sync/getting-started/

## 5. Seçim ve kopyalama UX

- Bir kez dokunma: kelime seç / tercihe göre iki seçenek.
- Uzun basma: seçilebilir metin; mavi tutamaçlar; sürükleme ile aynı cümlede veya satırlarda seçim.
- Üstte küçük menü: `Kopyala`, `Çevir`, `Paylaş`; sayfayı örtmeyen konum.
- Kopyala: doğru Unicode, harekeler dâhil, Arapça için yön bilgisi.
- Çevir: ana ekranda seçilen hedef dile. İlk olarak onaylı kelime/cümle sözlüğü, yoksa çevrimdışı MT.
- Mevcut `OnDeviceWordTranslator` ML Kit: ilk kullanımda internet/Wi-Fi ile model indirir, sonrasında cihazda çevirir. Bu **ilk kurulumdan itibaren sıfır-internet** değildir. Google atıf ve cihaz kullanım koşulları korunmalı. Kesin ilk kurulum çevrimdışı istenirse yerel paketlenebilir farklı, lisanslı çeviri motoru gerekir.

https://developers.google.com/ml-kit/language/translation/android
https://developers.google.com/ml-kit/language/translation/translation-terms

## 6. Mimari yerleşim / mevcut sistemi koru

Yeni `ReadingTextEngine`:
- `PdfDocumentInspector` — metinli/görsel/karma sayfa tespiti
- `NativePdfTextAdapter` — gerçek PDF char/text seçim yolu
- `OfflineOcrAdapter` — taranmış belgeler için bir defalık yerel OCR
- `WordGeometryRepository` — SQLite FTS5 + doğrulanmış kelime/bbox
- `TextRangeSelectionController` — kelime/çok kelimeli aralık, LTR/RTL, tutamaçlar
- `SelectionToolbar` — Kopyala/Çevir
- `LocalTranslationProvider` — doğrulanmış sözlük + indirilen model

Şu anki `StableReaderActivity`, `SinglePageSource`, `PageStore`, arama ve favoriler etrafına ince bir adaptör konur. Mevcut 605 sayfa görüntüsünü veya imza anahtarını değiştirme; ayrı geliştirici dalı/PR üzerinde çalış, ekran regresyonu olmadan birleştirme.

## 7. Zorunlu kabul testleri

1. Metinli PDF: seçilen Türkçe ve Arapça kelime gerçek metinle eşleşsin; 1/3/10 kelimelik aralık kopyalanabilsin.
2. Görsel PDF: 605 sayfada rastgele örneklenmiş dokunma noktaları, OCR/kanonik karakter, satır, yön ve kutu hizası insan kontrolünden geçsin.
3. RTL seçim: Arapça harekeler, farklı satırlara taşan ayetler, sayı ve noktalama, uzun basma, tutamaklar.
4. Yakınlaştırma, kaydırma ve ekran döndürmede `bbox ↔ ekran` dönüşümü aynı kelimede kalsın.
5. Çevir: TR/EN/DE/ES/ZH modelleri (gerekli ilk indirme ve internet kesintisi), yerel sözlük önceliği, doğruluk uyarısı.
6. 0.2.8/0.3.1 üstüne güncelleme, favori koruma, imza, zipalign, 16KB, Android 15.
7. Kur’an için hiçbir yanlış harf/ayet kopyalayan veya yanlış çeviri kaynağı gösteren hata yayın sürümüne geçirilmesin.

## 8. Araştırma / SDK kaynakları

- Android PdfRenderer.Page: https://developer.android.com/reference/android/graphics/pdf/PdfRenderer.Page
- Android Jetpack PDF viewer: https://developer.android.com/develop/ui/views/layout/pdf/implement-pdf-viewer
- AndroidX release notes (OCR provider APIs): https://developer.android.com/jetpack/androidx/releases/pdf
- Tesseract OCR Apache 2.0: https://tesseract-ocr.github.io/tessdoc/Installation.html
- ML Kit text recognition scripts: https://developers.google.com/ml-kit/vision/text-recognition/v2
- MuPDF Android **AGPL/commercial lisans** olduğu için varsayılan ücretsiz/proprietary uygulamaya lisans doğrulamadan dahil edilmez: https://mupdf.readthedocs.io/en/latest/guide/using-with-android.html
