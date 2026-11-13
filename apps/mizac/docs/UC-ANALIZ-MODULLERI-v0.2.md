# MİZAÇ analiz modülleri — v0.2 hazırlık

Bu belge `RASASER-FREE/apps/mizac/` içindeki **gerçek uygulanmış** modüllerin kapsamını ve bilimsel sınırlarını açıklar.

## 1. Fotoğraf / kısa video — yerel yüz geometrisi

- Kaynak: `FaceGeometryScreen.kt`
- Cihazda gömülü Google ML Kit Face Detection 16.1.7; indirmeli Play Services modelini kullanmaz.
- Fotoğraf uygulamanın özel dosya alanından okunur, EXIF yönü düzeltilir; video varsa 30 saniyeyi aşmayan videodan **bir temsilî kare** alınır. Gerçek zamanlı video izleme veya duygu analizi bu aşamada yoktur.
- Yüz algılanınca yüz dikdörtgeni piksel boyutları, genişlik/yükseklik oranı, gözler arası oransal mesafe, burun-ağız oransal mesafe ve baş yönü/dönmesi gösterilir.
- Ölçümler kamera açısına göre değişen 2B görüntü değerleridir. Mutlak yüz ölçüsü, biyometrik kimlik tanıma, cinsiyet, ırk, sağlık/duygu veya karakter tayini değildir.
- Sonuç yalnız RAM'de gösterilir ve uzaktan yüklenmez. Kullanıcı fotoğraf veya videoyu silmek için gizlilik ekranını kullanabilir.

**Teknik kaynak:** https://developers.google.com/ml-kit/vision/face-detection/android

## 2. Erzurumlu İbrahim Hakkı — kıyâfetnâme tarihi

- Kaynak: `MarifetnameScreen.kt`
- Baş, yüz, göz, alın, kaş, saç ve burunla ilgili **tarihî düşünce geleneği** kategorileri açıklanır.
- Tüm bilgi kartlarında **tarihî görüş / bilimsel kanıt değil** çerçevesi korunur.
- Uygulama herhangi bir kişinin yüz ölçülerini tarihî karakter etiketlerine bağlamaz. Tarihî ifadeler kullanıcının yüzüne göre filtrelenmez.
- Kaynak: TDV İslâm Ansiklopedisi, “İbrâhim Hakkı Erzurûmî” (https://islamansiklopedisi.org.tr/ibrahim-hakki-erzurumi); Mârifetnâme ilm-i kıyâfet bölümü ve TDK Belleten kıyâfetnâme türü araştırması.

## 3. Kişilik soruları ve sonuçlar

- Kaynak: `PersonalityTestScreen.kt`
- **10 soru**: TIPI — Gosling, Rentfrow & Swann (2003). 1–7 cevap, orijinal 5 alan x 2 madde ve ters kodlama korunur; Türkçe ifadeler ön çeviri olduğu için Türkçe bağımsız geçerlik veya nüfus normu iddia edilmez.
- **50 soru**: Goldberg/IPIP Beş Faktör 50 — 5 boyut x 10 soru, özgün olumlu/ters anahtar korunur; Türkçe ifadeler ön çeviri olup ayrıca psikometrik çalışma gerektirir.
- **120 soru**: IPIP-NEO-120'nın özgün 120 madde dizisi, 30 alt alan ve Türkçe ölçek kontrolü **henüz sağlanmadığından** bu seçenek sonuç üretmeyecek biçimde açıkça bilgilendirilmiştir.
- Kişilik puanı yalnızca kişinin **kendi beyan ettiği yanıtlar** üzerinden hesaplanır, yüz/veri analizinden **asla puana ekleme yapılmaz**.
- IPIP-50 ve TIPI puanları alan ortalaması olarak gösterilir; klinik teşhis, 'kesin karakter' hükmü ve nüfus yüzdeliği kullanılmaz.
- Sonuçlar cihaz ekranında hesaplanır; soru yanıtları kalıcı veri tabanına henüz kaydedilmez. Analiz geçmişi bu aşamada yoktur.
- Kaynak: https://gosling.psy.utexas.edu/scales-weve-developed/ten-item-personality-measure-tipi/
- Kaynak: https://ipip.ori.org/newBigFive5broadKey.htm
- Kaynak: https://ipipneo.org/scoring/

## Kullanıcı verileri ve imza

- Android Manifest'te INTERNET izni yok; hesap, sunucu, reklam/analitik SDK'sı yok.
- `allowBackup=false`; medya uygulamanın özel dosyalarında, soru tercihleri SharedPreferences'ta tutulur.
- Gizlilik ekranında yerel MİZAÇ dosyalarını silme işlevi var. Kullanıcının galerisindeki asıl dosyaları silmez.
- RaSaSeR FREE deposu public kalır, imza anahtarları veya kullanıcı verileri depoya yüklenmez.
- PRIVATE üretim ve imzalı AAB sonraki aşama.
- Bu aşamada kod GitHub'a işlendi; **Android üzerinde gerçek cihaz/derleme testi henüz doğrulanmadı**.

## Sonraki sağlamlaştırma
- Kullanıcının onaylı siyah-altın logo görselini uygulamaya özel kaynağa doğrulanmış biçimde ekleme.
- Ekran taşması, yatay/dikey, Android 15 cihaz testi, ML Kit bağımlılıkları ve aygıt testleri.
- 120 soruluk ölçeğin doğrulanmış veri seti, 30 alt alan puanlama ve dil uyarlama.
- Soru sonuçları için isteğe bağlı cihaz içi şifreli geçmiş ve tam silme.
