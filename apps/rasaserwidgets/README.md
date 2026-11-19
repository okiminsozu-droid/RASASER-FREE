# VARYOK — RaSaSeR • APPS & TECHNOLOGY — 0.3.2

Bu sürüm, `RASASER-FREE/apps/rasaserwidgets/` projesi içinde önceki Android paket kimliği
`com.rasaser.free.rasaserwidgets` korunarak geliştirildi. Diğer RaSaSeR uygulamalarına,
Cloudflare'a ve www.okiminsozu.com sitesine dokunulmadı.

## Onaylanan ana sayfa
Pembe arka plan, açık pudra tonlu ana **Widget Oluştur** butonu ve gönderilen
orijinal siyah-altın VARYOK / RaSaSeR logosu korunur. Sağ üstte bayraklı altı dil
(TR/EN/DE/ES/JA/AR) ve ayarlar simgesi vardır. En altta www.okiminsozu.com ve
**ⓘ gizlilik** ikonu bulunur. Gizlilik ikonu seçilen dilde doğru veri açıklamasını gösterir.

## Widget Oluştur — yalnız telefondaki uygulamalar
Arama ve alfabetik liste vardır. Her sırada telefondaki uygulamanın **gerçek ikonu,
adı ve sağ ok** yer alır. Fotoğraf, şeffaflık, web ve araç seçenekleri artık üst üste
tuş olarak listelenmez. Android'in LAUNCHER görünürlük kurallarına uygun uygulamalar,
yeni kurulum/kaldırma sonrası ve ekran yeniden açılınca güncellenir; liste saklanmaz.
Uygulamaya geniş paket envanteri izni eklenmemiştir.

Uygulamaya dokunulduğunda alttan özelleştirme penceresi açılır:
- Uygulamanın gerçek ikonu ve adıyla ön izleme
- Varsayılan simge / **Özel Fotoğraf** seçimi (Android sistem fotoğraf seçicisi)
- %0–100 şeffaflık / görünürlük kaydırıcısı
- Oluştur → cihazın ana ekranına widget ekleme isteği; Android'in onayı gerekir

Özel fotoğraf, widget ID oluşturulmadan önce yalnız geçici dosyada tutulur ve
kullanıcı kaydettiğinde ilgili widget'ın cihaz-içi özel dosyasına taşınır.
Uygulama ikonu varsayılan widget görüntüsüdür. Düzenleyicinin mevcut araçları
(uygulama/web/arama/not/Wi-Fi/Bluetooth/ayarlar/fener, etiket, fotoğraf ve
opaklık) **korunur** ve cihazın sistem Widget'lar menüsünden de kullanılabilir.

## Ayarlar
Ayarlar simgesi **yalnız oluşturulmuş widget'ların sıralı listesini** açar.
Her satırda widget ikonu/fotoğrafı, adı ve yanında çöp kutusu bulunur.
Satıra dokunarak mevcut widget düzenlenebilir. Ayarlarda başka menü,
gizlilik yazısı veya ek buton bulunmaz. Silmek, yerel widget ayarlarını ve
fotoğrafını temizler, widget'ı boş hale getirir. **Android ana ekranındaki
widget kutucuğunu programatik olarak kaldırma iznimiz yoktur**: onun kaldırılması
ana ekranda uzun bas → Kaldır işlemi ile yapılır; silme onayında belirtilir.

## Çevrimdışı ve ücretsiz model
INTERNET, genel depolama, takip ve reklam izni/SDK'sı yoktur. Yalnız seçilen
fotoğraflar, hedefler, başlıklar, opaklık ve üç ücretsiz widget sayacı cihazda
saklanır. Üç başarılı yeni widget oluşturma ücretsizdir; mevcut widget'ı
düzenlemek hak tüketmez, silmek hakkı geri kazandırmaz.
**Google Play ödeme altyapısı henüz bağlı değildir**; Premium kilidi gerçek
satın alma gibi gösterilmez.

## Test ve yayın
Sürüm 0.3.2 bir debug/test kaynağıdır. Gerçek Android cihaz testi, animasyon
ve çeşitli launcher uyumluluğu ayrıca doğrulanmalıdır. Release imza anahtarı
PUBLIC depoya eklenmez. GitHub test iş akışı sadece VARYOK modülünü derler.
