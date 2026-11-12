# Kur’anı Keşfet — 304 sayfalık çevrimdışı içerik paketi

**Kaynak:** Kullanıcının sağladığı `Renkli_Kelime_Altli_Kuran_TAMAMI_Calisma_Nushasi.pdf`, 304 sayfa, 90.889.211 bayt.

**SHA-256:** `387674c9a3bfb2c501c2dcca4b63946ea05f386dcd4d6fa4b63b4060b01397ea`

## Paket dizinleri

Uygulama derlenmeden önce, özel içerik ZIP'indeki aşağıdaki dosyaların tam olarak bu yerlerde olması gerekir:

- `apps/kurankelime/app/src/main/assets/quran/kelime_mealli.pdf`
- `apps/kurankelime/app/src/main/assets/quran/word_ocr.sqlite`

PDF kullanıcıdan seçilmez. `PdfReadActivity` kurulumda gömülü PDF'yi cihazın özel depolamasına çıkarır, SHA-256 ve 304 sayfa sayısını denetler. `BundledWordIndex` yine gömülü SQLite'ı açar, Türkçe/Arapça yerel kelime **OCR adayları** için FTS araması yapar ve dokunma koordinatlarından görsel vurgulamaya yardımcı olur. Hiçbir içerik uzak API'ye gönderilmez.

## Doğruluk ve sınırlar

Kaynak PDF'de ayet/kelime metinleri görsel olarak işlenmiştir; PDF'nin makineyle okunabilir metin katmanında yalnız sayfa üst/alt bilgileri vardır. `word_ocr.sqlite` içindeki `ocr_tokens` tablosu, Türkçe ve Arapça metin *adayları* ile sayfa ve koordinatlarını kapsar; bu kayıtların hiçbirinin doğrulanması varsayılmaz. `verified_words` ve `verses` doğrulanmış bağıntılar eklenene kadar boş kalabilir.

OCR adayı ile birebir Arapça→Türkçe kelime karşılığı eşitlemesi veya sure/ayet konumu **kanıtlanamaz**. Kısmi/yapay veri ile ayet numarası üretmek yasaktır. Mevcut PDF altındaki Türkçe metin görüntüsü doğrudan görünür. EN/DE/ES/ZH sözcük çevirisi yalnız ayrıca doğrulanmış, lisansı uygun veri varlığında gösterilmelidir; Türkçeden otomatik tahminle doldurulmaz.

Bu nedenle bu derleme tam sayfa okuma ve sayfa düzeyinde OCR araması içindir; **kusursuz kelime meali / ayet bazlı arama henüz tamamlanmış sayılmaz.**

## Dağıtım güvenliği

90MB PDF ve Türkçe kaynak içeriği açık kaynak public depoya yüklenmez. Telif koşullarının ve kaynak gösterme gerekliliklerinin ayrıca denetlenmesi gerekir. Public GitHub Actions test APK'si bu içeriği barındırmaz; kullanıcının indirdiği özel içerik ZIP'iyle yerel/özel derleme yapılır. PDF'nin kullanıcıya tekrar seçtirilmesi hedeflenen nihai davranış değildir.

## 11 Kasım 2026 — içerik paketi ölçüm sonucu

Özel dağıtım dosyası: `Kurani_Kesfet_304_Sayfa_Cevrimdisi_Icerik_Paketi.zip` (106.337.183 bayt).

- 304/304 PDF sayfası; içeride 1.208 gömülü sayfa görseli ve 605 orijinal dizgi sayfası
- 144.453 Türkçe, 84.490 Arapça **OCR sözcük adayı** (farklı sayfalarda tekrarlanan kelimeler ve okuma hataları dahildir)
- 61.139 doğrulanmamış Arapça–Türkçe görsel komşuluk adayı
- SQLite bütünlük testi `ok`, PDF SHA-256 kontrolü başarılı
- Verifiye sure–ayet–kelime eşleştirmesi: **0**; EN/DE/ES/ZH çevirileri: **henüz sağlanmamış**

Bu sayılar asla 228.943 doğrulanmış Kur'an kelimesi veya 61.139 doğru çeviri olarak sunulmaz. Çevrimdışı altyapı çalışır, ancak tam ayet-konum ve beş dil kelime eşleştirme doğruluğu henüz tamamlanmamıştır.
