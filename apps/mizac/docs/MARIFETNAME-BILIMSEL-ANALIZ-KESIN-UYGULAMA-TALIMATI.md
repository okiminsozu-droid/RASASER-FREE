# MİZAÇ — KESİN UYGULAMA TALİMATI (13.11.2026)

## Kapsam ve değişiklik sınırı

**Yalnızca** Hızlı Analiz sonuç ekranı, normal (10 / 50 / 120 soru) analiz sonuç ekranı ve Mârifetnâme'ye ait kaynaklı görünüş katalogu işlenecek. Diğer tüm özellikler ve mevcut UI korunacak. Ana sayfa, butonlar, kum zemin, beyaz tuşlar, onaylı siyah-altın logo, TR/EN slogan, 5 dil ve AR RTL, web bağlantısı, fotoğraf/video ekleme, 30 saniye video sınırı, 10/50/120 özgün soru sırası, ilerleme ve yanıt kutuları, veri silme, çevrimdışı işleme, paket adı ve mevcut uygulamalar değiştirilmeyecek.

## 1. Hızlı Analiz

**Birinci bölüm: MÂRİFETNÂME — ESERDE GEÇEN TARİHÎ GÖRÜŞLER**

- Ana kaynak: Erzurumlu İbrahim Hakkı, *Mârifetnâme*, beşinci bölüm, dördüncü madde (baş, boyun, saç, alın, kaş, göz, yüz, burun, ağız, diş, çene, sakal); beşinci madde (diğer beden uzuvlarına ilişkin kıyâfet yorumları).
- Temel metin: https://tr.wikisource.org/wiki/Marifetname/42
- Her kayıt: **özellik/tasvir**, **eserin bu tasvir için ileri sürdüğü tarihî görüş**, **bölüm/madde kaynak kodu**. Eserin söylemediği sıfat veya sayısal eşik uydurulmayacak. Kayıtlar katalogda tek tek gezilebilir.
- Güncel sürümde yalnız 10 seçenek bulunuyor; önce resmî metinle tam karşılaştırma ve kapsam kontrolü yapılacak, ardından eksik yorumlar tamamlanacak. Doğrulanmamış veya yanlış çevrilmiş başlıklar eklenmeyecek.
- Boy, deri/saç rengi veya fotoğrafla kesin ölçülemeyen özelliklere kişisel sonuç ataması yapılmayacak. Özellikle tarihî metindeki karakter, ahlâk, zekâ, sağlık, suç ve benzeri iddialar kullanıcının biyometrik özelliğine dayanarak onun hakkında otomatik karar olarak sunulmayacak. **Tarihî iddianın esere göre ne olduğu eksiksiz gösterilebilir**; kullanıcı ile ilişkilendirilmesi ayrı bir kişilik hükmü olmayacak.
- Kayıtlar kompakt liste halinde, tek satır başlık ve ayrıntı açma biçiminde sunulacak. Tekrarlayan uzun modern yorum paragrafları kaldırılacak. Yalnız bir kez kısa `Tarihî kaynak, bilimsel teşhis değil` etiketi korunacak. Kaynak bağlantısı görünür kalacak.
- Diller: TR/EN/DE/ES/AR; uluslararası dillerde özgün metin alıntısı Türkçe ise "Türkçe özgün kaynak" ibaresi açıkça gösterilecek, doğru tercüme bulunmadan yorum uydurulmayacak. Modern telifli sadeleştirmelerden kopyalama yapılmayacak.

**İkinci bölüm: BİLİMSEL GÖRÜNTÜ ANALİZİ — TAM SAYISAL LİSTE**

Eğer ML Kit veya görüntü algoritması değeri döndürüyorsa *etiket, birim, değeri* göster:
1. Kaynak: fotoğraf veya videonun temsilî tek karesi (videoda hangi kare açıkça yazılacak).
2. Algılanan yüz sayısı.
3. Ölçülen görüntüde yüz dikdörtgeni piksel eni ve boyu.
4. Yüz bölgesi genişlik/yükseklik oranı.
5. İki göz arası mesafenin yüz dikdörtgeni genişliğine oranı.
6. Burun-alt ağız işareti mesafesinin yüz dikdörtgeni yüksekliğine oranı.
7. Baş Euler-Y ve Euler-Z açıları (derece).
8. ML Kit sınıflandırması mevcutsa gülümseme ve iki gözün açık görünme olasılıkları (yüzde).
9. Değer döndürmeyen alanlar: 'Ölçülemedi'. Uygun olmayan çekim açısı ve görüntü kalitesi notu kısa tutulacak.
10. Sadece gerçekten hesaplanırsa yeni görüntü metrikleri (ör. kadraja göre göz/burun hizası); sahte yüzde, kişilik puanı veya insan hakkında kanıtsız çıkarım yok.

Bu değerlerin yalnız **görüntü ölçümleri** olduğu açıkça yazılacak. Bir yüz fotoğrafından IQ, öğrenme becerisi, dürüstlük, kişilik, psikiyatrik veya tıbbi tanı hesaplayan **kanıtlanmış** API mevcut değildir. Sağlık/zekâ/karakter değerlendirmesi yapıldı iddiası kullanılmayacak.

ML Kit kaynak: https://developers.google.com/ml-kit/vision/face-detection/android

## 2. Normal Analiz

- 10/50/120 soruluk içerik aynen kalacak, hiçbir soru, dil veya puanlama anahtarı değiştirilmeden korunacak.
- Sonuç ekranında sıralama: **(1) Mârifetnâme tarihî kaynak katalogu (kendi başına) → (2) Kullanıcının soru yanıtlarından yapılan sayısal özet**.
- 5 kategori için yalnız kategori adı + kullanıcı yanıtlarının ters-kodlama uygulanmış aritmetik ortalaması (1–5) + cevaplanan madde sayısı. Kategoriler: açıklık, düzen, sosyal enerji, iş birliği, duygusal denge.
- İstendiğinde tüm 10/50/120 soru metinleri ve verilen 1–5 yanıtlar liste halinde görülebilecek.
- Doğrulanmamış özgün MİZAÇ anketi bilimsel olarak valide edilmiş kişilik tanısı adıyla sunulmayacak. Sonuca 'başarılı', 'güvenilir', 'zeki', 'hasta' gibi öznel etiketler uydurulmayacak.
- Tarihî metindeki yüz yorumları test skorlarını etkilemeyecek; test cevapları da tarihî kıyâfet sonucu seçmeyecek.

## 3. Gizlilik ve korunacaklar

- Her görüntü, 30 saniyeye kadarki video ve cevap kullanıcı cihazında; sunucuya gönderme, kayıt toplama, analitik SDK veya ağ üzerinden analiz motoru yok.
- Tarihî katalog kaynakları uygulama içine metin olarak gömülü olacak; kaynak linkine ancak kullanıcı tıklarsa tarayıcı açılacak.
- Eski stabil dosyalar korunur, yalnız `apps/mizac/` içindeki gerekli kaynaklar işlenir. Eski diğer uygulamalara ve özel `RASASER` deposuna dokunulmaz.
- Mârifetnâme eski eser olmakla birlikte modern baskı, çeviri ve internet transkripsiyonlarının ayrı telif/lisans koşulları bulunabilir. Uzun internet metni kopyalanmaz; kaynak başlığını ve bağımsız doğrulanmış özeti kullan.
- Bir ölçüm veya tarihî yorum bulunamazsa gösterilmemesi, **uydurulmuş sonuçtan tercih edilir**.

## 4. Değişiklik kabul testleri

- TR/EN/DE/ES/AR ekran, katalog ve sayısal ölçümler kontrol edilir; AR RTL ve sağ üst bayrak konumu korunur.
- Fotoğraf ile hızlı analiz: tarihî başlık önce, ölçümler ikinci. Video: aynı sıra; 30 saniye kontrolü korunur.
- Fotoğrafta yüz yok: 'Yüz tespit edilemedi' ve yalnız ilgili kaynak listesi, uydurma sonuç yok.
- 10/50/120 soru testleri: sorular sırasıyla, 1–5 kutuları ile ileri kaydırma; sonuçlar ve cevaplar eksiksiz.
- Android 15 üst durum çubuğu/alt navigasyon taşması yok; logo ve linkler korunmuş.
- Başarılı Actions güvenlik ve derleme testi + APK/AAB bütünlük kontrolü yapılmadan 'test paketi hazır' denilmeyecek.

Bu belge kullanıcı talimatıdır; **tamamlandı iddiası değildir**. Tam uygulama ve cihaz doğrulaması ilgili PR'de ayrı kayıtlarla izlenecek.
