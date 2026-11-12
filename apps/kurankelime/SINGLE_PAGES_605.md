# KUR’ANI KEŞFET — 605 tam sayfa / v0.2.7 (2026-11-12)

## Neden 605 sayfa?

Kaynak `Renkli_Kelime_Altli_Kuran_TAMAMI_Calisma_Nushasi.pdf` 304 PDF sayfasıdır, fakat PDF 3–303'teki dört dikdörtgen görüntü aslında **iki tam, ardışık okuma sayfasının üst ve alt metin bölümleridir**.

- PDF 1–2 → birer tekli sayfa (toplam 2)
- PDF 3–303 (301 kaynak sayfa) → her biri iki tekli sayfa (602)
- PDF 304 → son tekli sayfa (1)
- **Toplam 605 tam okuma sayfası ve 1.208 orijinal metin görüntüsü**

Tekli sayfalar sırayla üst-sol + üst-sağ; sonra alt-sol + alt-sağ şeklinde birleşir. Önceki v0.2.6'nın 1.208 büyütülmüş parça düzeni artık kullanılmaz.

## Gerçek görüntü kalitesi

Kaynağın görselleri yaklaşık 546–572 piksel genişliğinde. Tekli sayfalarda özgün görseller PDF'den doğrudan (PNG nesnelerinden) çıkarılarak **kayıpsız WebP** biçiminde birleştirilir. OCR ile Kur’an metni yeniden yazılmaz, yapay zekâ ile harf üretilmez veya piksel uydurulmaz. Yeni dosyalar yaklaşık 592 piksel genişliğindedir; bu **özgün kaynak ayrıntısını aşan gerçek yüksek çözünürlük anlamına gelmez**. Çok daha yüksek çözünürlük gerekiyorsa yüksek çözünürlüklü ayrı orijinal sayfa varlıkları gerekir.

Özel, herkese açık GitHub'a konulmayan içerik paketi:
`Kurani_Kesfet_605_Tekli_Sayfa_Kayipsiz_Gorsel_Paketi.zip`
- `quran/single_pages/page_0001.webp` … `page_0605.webp`
- `quran/single_pages/pages.json`

Paket dışarıdan yükletilmez; özel APK hazırlanırken doğrudan `app/src/main/assets/quran/single_pages/` altına gömülür.

## Kod ve geriye uyumluluk

- `SinglePageSource.kt` her görselin orijinal PDF konumunu `pages.json` aracılığıyla okur.
- `StableReaderActivity.kt` 605 tam sayfayı tek tek açar. Dikeyde tamamını kırpmadan sığdırır, yatayda genişliği doldurup dikey kaydırma sağlar.
- Arama sonuçlarının kelime koordinatları kaybolmaz; her hit doğru 605'lik sayfaya gider ve işaretlenebilir.
- `PageStore.kt` önceki PDF sayfa kayıtlarını ve 1.208 ekran ilerlemesini silmez; 605 sayfalık ilerlemeyi ayrı anahtarda tutar.
- Eski çevrimdışı SQLite OCR dizini, orijinal PDF arşivi, logo, arama/favoriler ayrı korunur.
- İmzalı offlineTest APK aynı özel test sertifikasıyla sürüm 0.2.7, versionCode 9 olarak hazırlanmıştır.
- Üretim mağaza imzası değildir, telefon üzerinde uçtan uca manuel test henüz yapılmamıştır.

## İçerik ve güven sınırları

Metinler kullanıcı kaynak PDF'sinden doğrudan gelir. OCR arama kayıtları doğrulanmış ayet-kelime eşleşmeleriyle eşdeğer sayılmaz. Önceki yaklaşık ayet aralıkları aynen korunur; yanlış sure/ayet numarası uydurulmaz.

PDF ile içerik ZIP'i ve imza anahtarı **public GitHub deposuna yüklenmez**.
