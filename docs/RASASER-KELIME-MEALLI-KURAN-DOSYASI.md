# RaSaSeR — Kelime Mealli Kur'an (BAĞIMSIZ PDF ve UYGULAMA)

**Proje yeri:** PUBLIC `okiminsozu-droid/RASASER-FREE/apps/kelime-mealli-kuran/`  
**Durum (11 Kasım 2026):** Kaynak bütünlüğü doğrulanmış 114 sure / 6236 ayet Arapça metin yüklendi. İlk renkli kelime dizgisi ve Fâtiha meali örneği hazırlandı. **77 bin civarı kelimenin tamamına ait Türkçe açıklamalar henüz telifsiz ve ilmî incelemeden geçmiş değildir.**

## Değişmez kurallar

1. Eski RaSaSeR uygulamalarına, PRIVATE depodaki motorlara ve imza anahtarlarına dokunma.
2. Mushaf metni 1:1 dondurulmuş ve kaynakla sürümlenmiş olacak. Arapça kelimeler ayrılarak gösterilse de harfler ve harekeler değiştirilmez.
3. Aynı offline kaynaktan (a) PDF, (b) Android SQLite/FTS5, (c) masaüstü okuma hazırlanacak. Uygulama çalışmak için API/üyelik/şirket hesabı/ödeme istemeyecek.
4. Arapça metin lisans bildirimleri kaldırılmaz. Kaynakta açık izin yoksa bir başkasının Türkçe kelime mealini tamamıyla kopyalama, yeniden lisanslama veya PUBLIC depoya koyma.
5. Arapça kelime <-> Türkçe mana konum eşlemesi ayet ve kelime numarasıyla kilitlenir. Ayet meali birebir kelime karşılığı değildir.
6. PDF yazılarını yüksek kaliteli Arapça Unicode font ile üret, metin PDF içinde seçilebilir olsun, baskı ve ekran görünümünü test et.
7. Sure ve ayet sayısı daima **114 / 6236**; pozisyon, boş alan, durak işaretleri ve ek basmala girişleri ayrı doğrulanır.
8. Arapça verinin SHA-256'sı ve yazarın gözden geçirdiği Türkçe kelime veri sürümü imza kayıtlarıyla yayımlanır.
9. "Tamamlandı" demeden önce dinî ilimler uzmanıyla örnekleme değil, sistematik metin ve kelime anlamı doğrulaması yapılır.

## Doğrulanmış Arapça metin

- Kaynak: **Tanzil Quran Text (Uthmani, v1.1)**, https://tanzil.net/
- Lisans: **CC BY 3.0**, https://tanzil.net/docs/Text_License
- Bu kaynak bir şirketin canlı servisine bağımlılık yaratmaz: metnin tamamı burada yerel JSON olarak saklıdır.
- Metnin değiştirilmemesi, kaynağın belirtilmesi ve bağlantısının korunması zorunludur.
- Dondurulmuş dosya: `data/quran-uthmani-tanzil-v1.1.json`, 114 sure, 6236 numaralı ayet.
- İşlenmiş kaynak indekslemesi: https://github.com/dotquran/corpus (Tanzil'e atıf korunur).

## Türkçe kelime meali hakkında telif bulgusu

Açık kaynak yazılım ile açık lisanslı çeviri **aynı şey değildir**. İncelenen `skonuksal/turkce-fonetik-kurani-kerim` kodu MIT'tir; ancak projenin kendisi veri lisanslarının Tanzil, QuranWBW, Açık Kuran ve Diyanet hak sahiplerine ait olduğunu belirtir. Dolayısıyla o verileri doğrudan alıp "tamamen telifsiz Türkçe meal" diye yayımlamıyoruz.

Doğru yaklaşım: RaSaSeR'ın özgün Türkçe kelime karşılıklarını üretmek veya tam ticari/ticari olmayan yeniden dağıtım izni ve kapsamı açık veri kullanmak. Gerekli inceleme belgesi kayıt altına alınmadan tam meal PDF'si veya uygulama yayına gönderilmez.

## PDF üretimi

- `render_pdf.py`: API çağrısı yapmayan, tamamen yerel `data/` klasörünü kullanan Python/WeasyPrint dizgicisi.
- `--mode full-arabic`: 114 surenin eksiksiz Arapça metnini kelime bazında renkli düzenle gösteren PDF (Türkçe boş olan kısımlar bilinçli olarak tamamlanmamış işaretlenir).
- `--mode sample-fatiha`: Özgün editoryal Türkçe kelime karşılıklarıyla 7 ayetlik ilk tasarım örneği. **Dinî inceleme için örnektir; onaylı tercüme değildir.**
- `.github/workflows/08-quran-colored-word-pdf.yml`: ücretsiz Actions Linux derlemesi.

## Gelecek veri sözleşmesi

Her satır: `{surah:int, ayah:int, word:int, arabic:string, meaning_tr:string, reviewed_by:string, review_date:string, licence:string}`.

Tüm 6236 ayetin kelime sayısı eşleşmeden ve çeviri alanları boş kalmadan “tam Türkçe kelime meali” üretimi kapalı tutulur. Bir veri üretici firma/API hesabına bağlı sistem kurulmaz.

## Kaynaklar ve lisans teyidi

- https://tanzil.net/docs/Text_License
- https://github.com/dotquran/corpus
- https://github.com/skonuksal/turkce-fonetik-kurani-kerim
- https://github.com/acik-kuran/acikkuran-api (API CC BY-NC-SA 4.0; bu lisans ücretli ticari projelere otomatik izin vermez)

Bu belge teknik çalışma kaydıdır. Bir yazılımın veya çeviri veri setinin telifsizliğini hukukî inceleme olmadan garanti etmez.
