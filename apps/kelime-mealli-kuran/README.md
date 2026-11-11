# RaSaSeR — Kelime Mealli Kur’an PDF Fabrikası

Bağımsız ve internet gerektirmeyen Kur’an PDF dizgi projesi. **Bu dal tamamlanmış Türkçe kelime meali değildir.**

## Doğrulanmış mevcut çıktı
- **Arapça:** 114 sure, 6236 numaralı ayet; kaynağı ve sürümü dondurulmuş Tanzil Uthmani v1.1.
- **Arayüz:** Her Arapça kelime sırasıyla renkli, seçilebilir Unicode yazı olarak basılır.
- **Fâtiha örneği (renk eşleşmeli):** 7 ayette 29 özgün taslak Türkçe kelime karşılığı bulunur. Her Türkçe karşılığın rengi üstündeki Arapça kelimeyle **birebir aynıdır**. İlmî editör onayı bekliyor.
- **Çalışma tablosu:** Yaklaşık 77.881 Arapça kelime girdisi, 4.364 durak işareti ve 114 sure için Türkçe anlam, izin, çevirmen ve hakem alanları.
- **Tam kelime mealli baskı:** Ancak bütün Türkçe karşılıklar doldurulup lisans ve ilmî kontrollerden geçtikten sonra yayımlanır.

Kalıcı önizleme arşivi:
https://github.com/okiminsozu-droid/RASASER-FREE/releases/tag/quran-pdf-preview-v0.1

## Kaynak ve telif

Arapça metin: Tanzil Quran Text (Uthmani v1.1), CC BY 3.0, https://tanzil.net/docs/Text_License. Metin değiştirilmez, kaynak gösterilir.

Türkçe kaynaklardan lisansı belirsiz veri kopyalanmaz. **Kodun açık lisansı bir çevirinin kullanım izni anlamına gelmez.**

## Bilgisayarda çevrimdışı üretim

Python 3.11+, WeasyPrint 66, Amiri ve DejaVu Sans fontları yeterlidir. Bütün veri dosyaları yereldir.

```bash
python render_pdf.py --mode sample-fatiha --out output/fatiha-ornek.pdf
python render_pdf.py --mode full-arabic --out output/arabic-complete.pdf
python generate_word_workbook.py --out output
```

Her proje bağımsızdır; CAR LAUNCHER, VARAK, KATİP, BENİMLESİN, SAYHA, EZAN veya PRIVATE RaSaSeR motorlarıyla kaynak bağlantısı kurulmaz.

## Yakın aşamalar

1. Açık ve uygun haklara sahip Türkçe kelime sözlüğü veya tamamen özgün Türkçe açıklama metni.
2. Her kelimenin konuma göre tek tek kontrolü ve gereken bağlam açıklamaları.
3. Bütün Türkçe anlamları alan tam alt-alta renkli A4/mobil PDF baskısı.
4. Aynı doğrulanmış metinden bağımsız SQLite Android uygulaması.

## Renkli Türkçe karşılık kontrolü (11 Kasım 2026)

- Arapça renk paleti: `#157e76`, `#a66b31`, `#456db3`, `#98659a`, `#4f8260`, `#a55650`.
- `render_pdf.py/words_html()`: Türkçe anlamı aynı renk değerinden üretir; Arapça sözcük ile Türkçe anlam daima aynı kelime kutusundadır.
- Kaynak çalışma tablosunda **77.881** Arapça sözcük girişi, **4.364** durak işareti, **29** taslak Türkçe karşılık ve **77.852** henüz boş Türkçe karşılık vardır. Bu nedenle tam Türkçe renkli kelime mealli Kur’an henüz nihai eser değildir.
- Hakları açık olmayan üçüncü taraf Türkçe meal verisi otomatik kopyalanmaz. Tam Kur’an için kelime karşılıklarının tamamının tamamlanması ve dinî ilimler incelemesi zorunlu.

## Türkçe renkli kelime baskısı — ilk genişletme (11 Kasım 2026)

- **Renk eşleştirme:** Arapça kelime ile hemen altındaki Türkçe kelime karşılığı aynı renk ve aynı kutu içinde.
- **Kapsam:** Fâtiha (1), Bakara 1–7 (2), Asr-Nâs (103–114) seçkisi; 14 sure seçimi, 388 kelime/karşılık eşlemesi, 87 ayet/besmele bölümü.
- **PDF oluşturma:** `python render_pdf.py --mode word-meaning-draft --out output/kelime-meali-taslak.pdf`.
- **Özgün editoryal Türkçe sözlük:** `data/turkish-word-meanings-editorial-draft.json` (Fâtiha kelimeleri PDF betiğinin içinde). Bu sözlük **taslaktır**, ilmî incelemesi yapılmadı.
- **Ücretsiz Actions:** `.github/workflows/08-quran-colored-word-pdf.yml` aynı kaynakla renkli çift dilli PDF yanında tam Arapça Kur’an PDF'sini üretir.
- **Önemli:** Bu seçim tam 114 surenin Türkçe kelime meali olarak sunulmaz. Tüm Kur’an meali henüz tamamlanmadı. Üçüncü taraf Türkçe çeviri kopyalanmadı.
