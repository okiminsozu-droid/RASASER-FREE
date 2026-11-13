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
