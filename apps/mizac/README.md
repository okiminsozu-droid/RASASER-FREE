# MİZAÇ — RaSaSeR kişilik farkındalığı

**Yayıncı:** RaSaSeR APPS & TECHNOLOGY  
**Çalışma alanı:** `okiminsozu-droid/RASASER-FREE/apps/mizac/`  
**Paket:** `com.rasaser.free.mizac`  
**Platform:** Android 8+ (minSdk 26); hedef Android 15 (API 35)  
**Arayüz:** kum rengi zemin, beyaz premium tuşlar, onaylı siyah-altın logo, sabit iki satır TR/EN slogan ve aktif `www.okiminsozu.com` bağlantısı.

## Beş dil — bayrak menüsü

Ana sayfada sağ üstte açılır bayrak listesi: **🇹🇷 Türkçe, 🇬🇧 English, 🇩🇪 Deutsch, 🇪🇸 Español, 🇸🇦 العربية**.

Dil seçimi `SharedPreferences` içinde cihazda saklanır; arayüz, fotoğraf/video alanları, geometrik ölçüm, tarihî içerik, 10/50/120 soru, sonuçlar, silme diyalogları ve gizlilik yazıları dinamik biçimde dil değiştirir. Arapça için `LayoutDirection.Rtl` uygulanır. Slogan kullanıcının onayladığı şekilde Türkçe ve İngilizce iki satır korunur.

## Özgün MİZAÇ soruları

**Kesin telif sahipliği garantisi verilmez; ancak bu projede modern testlerden kopyalama yapılmadan yeni, özgün soru ifadeleri ve beş dile bağımsız çevirileri hazırlanmıştır.** Hiçbir ücretli, lisanslı kişilik soru ölçeğinden madde aktarmayız.

- `questionbank/O.txt`: Yeniliğe açıklık; 24 özgün soru × 5 dil
- `questionbank/C.txt`: Sorumluluk ve düzen; 24 × 5 dil
- `questionbank/E.txt`: Sosyal enerji; 24 × 5 dil
- `questionbank/A.txt`: Uyumluluk ve anlayış; 24 × 5 dil
- `questionbank/S.txt`: Duygusal denge; 24 × 5 dil
- 10 soruluk: kategori başına ilk 2 soru
- 50 soruluk: kategori başına ilk 10 soru
- 120 soruluk: kategori başına tüm 24 soru

Cevaplar 1–5 aralığında; ters maddeler dönüşümlü hesaplanır. Sonuç her alanda öz değerlendirme ortalamasıdır. **Bu yeni soru dizilerinin klinik veya psikometrik geçerliği henüz test edilmemiştir. Bunları TIPI, IPIP ya da başka valide edilmiş ölçek diye tanıtmak yasaktır.** Normlar, nüfus yüzdelikleri, teşhis, işe alım, yalan tespiti veya kesin karakter hükmü üretilmez. Bilimsel ölçek eklenirse ancak özgün izin/puanlama/dil geçerlik denetiminden sonra ayrı modülde tanıtılır.

## Mârifetnâme ve özgün tarihî düşünme soruları

- `i18n/history.txt`: 8 çift başlık/açıklama, beş dil; Mârifetnâme'nin kıyâfetnâme ve dört mizaç tarihî kavramlarını özgün anlatım olarak açıklar.
- `i18n/history_reflection.txt`: Ahlâk, niyet, sabır, kendini tanıma üzerine **MİZAÇ'ın bağımsız yazdığı 10 soru**, beş dil.
- Bunlar İbrahim Hakkı'nın eserinden birebir soru, modern baskılardan paragraf veya modern çeviri değildir. Tarihî geleneğin fikrî bağlamını açıklamak amacıyla oluşturulmuştur.
- Fotoğraftan yüzün/bedenin görünüşünü ahlâka, karaktere veya zekâya bağlama **yok**. Fotoğraf/veri algoritması, kişilik anketi puanlarını değiştirmez.

Başlıca kaynaklar:
1. Tarihî eser: İbrahim Hakkı Erzurûmî, *Mârifetnâme* (1757), kıyâfet-nâme.
2. TBMM Açık Erişim Kütüphanesi tarihî nüsha: https://acikerisim.tbmm.gov.tr/items/37ceaca9-2499-4d5b-a0df-be6a757513c9
3. Diyanet İbrahim Hakkı biyografi ve tarihî değerlendirmeler.

## Geometri ve veri gizliliği

- Fotoğraf veya en fazla 30 saniyelik video isteğe bağlı.
- Gömülü `com.google.mlkit:face-detection:16.1.7` görüntüdeki yüz dikdörtgenini, göreli göz ve burun/ağız noktalarını ve baş açısını **yalnız telefonda** ölçer. Tıbbi/kişilik sonucu değildir.
- Medya yalnız `context.filesDir` içinde, soru sayısı ve dil yerel tercihlerde; cevaplar yalnız geçici oturum durumunda.
- Android `INTERNET` izni, hesap, bulut, reklam/analitik SDK'sı ve dış kişilik API'si **yoktur**.
- `allowBackup=false`; kullanıcı gizlilik ekranından yerel MİZAÇ kayıtlarını silebilir. Fotoğraf/videosunun asıl galeri kopyası etkilenmez. Telefonun kendi kamera veya tarayıcı davranışları ayrı işlem olabilir.

## Geliştirme ve koruma

- Öteki uygulamalara dokunulmaz, her uygulama ayrı klasör/paket.
- Genel kod yalnız `RASASER-FREE` deposunda; imza anahtarı, kullanıcı verisi ve özel motor kodları buraya eklenmez.
- Kaynak tamamlanıp fiziksel cihazda test edildikten sonra güvenli `RASASER` PRIVATE depoya aktarılacak; üretim imzası yalnız PRIVATE ortamında yapılacak.
- Derleme: `.github/workflows/mizac-android-build.yml` yalnız `apps/mizac/` uygulamasını derler.
- Mevcut durum: Çok dilli özgün anket + tarihî bölüm + yüz geometrisi entegre edildi; Kotlin derleme ve kullanıcı arayüzü testleri tamamlanmadan yayın hazır denilemez.

## v0.3 — Mârifetnâme önce, görüntü ölçümü sonra

- **Hızlı Analiz** yalnız cihazda `mizac_photo.dat` veya `mizac_video.dat` dosyalarını inceler. Her ikisi varsa **son eklenen** medya seçilir.
- Sonuç ekranında **1. Mârifetnâme'nin tarihî kıyâfet yorumları** bağımsız beş dilde, kaynaklı bilgi kartları olarak gösterilir. Bunlar görüntüye göre karakter, sağlık veya zekâ tanıları **değildir**; kişinin yüzüne göre bir tarihî etikete karar verilmez.
- Ardından **2. Bilimsel görüntü ölçümleri**: yüz piksel alanı, iki boyutlu genişlik/yükseklik, göz/burun/ağız arası göreli mesafeler, baş dönüşü/eğimi, mümkünse ML Kit gülümseme ve göz açıklığı **görüntü tahminleri**. Sonuncular duygunun, huyun, sağlık durumunun veya zekânın göstergesi olarak sunulmaz.
- Sağlık: yüz görüntüsünden tanı veya risk puanı verilmez. Zekâ: görüntüden IQ/öğrenme kapasitesi verilmez. Kişilik: fotoğraftan puan verilmez; ayrı öz değerlendirme soruları doğrulanmış psikometrik test olarak tanıtılmaz.
- Video görüntüsü tek temsilî orta kareden okunur; 30 saniyelik videonun tamamında süreklilik veya hareket takibi **henüz yoktur**. Kayıt ve veri aktarımı sadece kullanıcının kendi cihazında gerçekleşir.
- Bu sürüm `versionCode=3`, `versionName=0.3.0` olarak ayrı debug-test paket kimliğiyle derlenir.
- Başlıca kaynaklar: Google ML Kit Face Detection [resmî dokümantasyon](https://developers.google.com/ml-kit/vision/face-detection/android), TDV İslâm Ansiklopedisi [İbrâhim Hakkı Erzurûmî](https://islamansiklopedisi.org.tr/ibrahim-hakki-erzurumi) ve [Mârifetnâme](https://islamansiklopedisi.org.tr/marifetname). Kişilik yüz tahmini güvenilirliği: [2024 fotoğraftan kişilik izlenimi araştırması](https://journals.sagepub.com/doi/full/10.1177/08902070231225728).

Görüntüye dayalı medikal cihaz iddiası, yüz/bedene göre ahlâk veya zekâ puanlama, sınıflandırılmış kişilik tanısı yapılmayacaktır.

## v0.10.0 — PDF kaynağı + çevrimdışı yüz ve beden ölçümleri

**Araştırma kaynağı:** Kullanıcının sağladığı 19 sayfalık `MIZAC_Marifetname_Tum_Bedensel_Ozellikler_Arastirmasi.pdf` (SHA256 `8ae2e81228901d2a5dca750ccaf704e730cbe881cfa6b92ee38635a23e43be75`). Bu PDF, Mârifetnâme'yi temel alan önceki araştırmanın *derlemesidir*, kritik edisyon veya tüm yazma nüshaların tek tek karşılaştırılması değildir.

**PDF kapsamı:** 4. maddeden 103 yüz/baş/saç/boyun kaydı; 5. maddeden 54 beden/uzuv kaydı; 6. maddeden 7 dörtlü güzellik başlığı ve metinde ayrıca sayılan 4 nitelik; 7. maddeden 5 değerlendirme ilkesi; 8. maddeden 56 sağ-sol seğirme kaydı; 10 beden mizaç belirtisi, 4 hılt ve 5 diğer organ konusu. Uygulamada `assets/history/catalog_*.tsv` olarak **248 kaynak satırı** korunur. Katalogdaki her madde, sadece PDF kaynak derlemesindeki metin olarak gösterilir.

**Fotoğraf yüz motoru:** Gömülü ML Kit face-detection 16.1.7 ile yüz bölgesi, baş açısı, göz merkezleri, burun/ağız mesafeleri ve kontur modu üzerinden göz/burun/ağız/kaş genişlik oranları ölçülür. Açı nedeniyle güvenilir olmayan gülümseme/göz açık tahminleri gizlenir.

**Fotoğraf beden motoru:** İnternetsiz ML Kit Pose Detection 18.0.0-beta5, bir karede 33 olası beden noktasını inceler; yüksek güvenle görüntüde olanlardan omuz/kalça/gövde oranları, omuz-kalça eğimi, diz ve dirsek açıları ve göreli kol/bacak uzunlukları listelenir. Kısmi/eksik vücut kadrajlarında görünmeyen oranlar uydurulmaz. Video için tek temsilî kare kullanılır, hareket veya seğirme takibi yapıldığı iddia edilmez. Gerçek santimetre veya boy tahmini yoktur. Yüz/beden motorları kullanıcı cihazında çalışır; sunucu, analitik SDK veya ücretli API yoktur.

**Hükümlerin kaynağı ve davranış bölümü:** Mârifetnâme'nin görünüş ve beden hükümleri katalogdan aynen aktarılır; yüz oranları veya eklem açıları `dürüstlük`, `zekâ`, `hastalık`, `kibir` gibi kişilik/tıp niteliklerine kesin olarak dönüştürülmez. PDF, şahsın negatif davranışını düzeltmeye yönelik tek tek uygulanabilir adımlar vermediğinden ona ait olmayan öğütler esere atfedilmez. Daha önce 176 soruluk öz değerlendirme tamamlanmışsa yalnızca kullanıcının cevaplarına göre `Kuranda_176_Iyi_ve_Kotu_Insan_Ozellikleri_Ayetli.pdf` kaynak başlıkları ve ayet atıflarıyla güçlü/çalışılacak davranışlar gösterilir; yeni öğüt uydurulmaz.

**Test:** CI'da `assembleDebug`, `testDebugUnitTest`, APK paket kontrolü, kaynak kataloğu/soru/çeviri bütünlüğü kapıları çalışır. Kullanıcının gerçek cihazında yüz ve beden modelinin tüm koşullarda doğruluğu ayrıca sınanmalıdır. Public repoya debug test paketi dışında production imza anahtarı yüklenmez.
