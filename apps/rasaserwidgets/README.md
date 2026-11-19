# RaSaSeR Widget Studio — v0.1.0

Bağımsız Android widget uygulaması. **Çevrimdışı çalışma, reklam yok, izleme yok, üyelik yok.** Sadece bu klasör içinde geliştirilir; diğer RaSaSeR projelerine veya özel motora bağlanmaz.

## Mevcut ilk sürüm işlevleri
- Her ana ekran widget'ı için **ayrı ayar**: resim, etiket, görünürlük (0–100), dokunma eylemi.
- Galeriden dosya seçme (Android'in sistem seçicisi), uygulama içi özel alana resim kopyalama. Geniş depolama izni yok.
- Widget boyutlandırma, yeniden düzenleme, uygulama içinden bağlı widget yönetimi.
- Dokunma eylemleri: uygulama açma, internet adresi (tarayıcı açılır; çevrimdışı olduğunda sayfa yüklenmeyebilir), telefon numarası arama ekranı, yerel not, Wi-Fi/Bluetooth/Sistem ayarları, izinli el feneri aracı.
- 0 görünürlükte tamamen şeffaf dokunma alanı, 100'de görünen fotoğraf/widget.
- Ağ, reklam, analitik, hesap veya bulut SDK'sı olmadan cihaz içi SharedPreferences + özel resim dosyaları.
- Ana ekran Android sürümüne/launcher'a bağlıdır; görünmez widget uygulama çekmecesindeki gerçek uygulama simgesini gizlemez.

## Kurulum
Android Studio ile **bu klasörü** ayrı proje olarak açın. Alternatif: depoda `Android FREE Derleme` iş akışında `app_slug=rasaserwidgets`. **Bu iş akışı kullanıcı onayı olmadan çalıştırılmayacak.** JDK17 / Gradle8.9 / Android SDK35 / AGP8.7.3 / Kotlin2.0.21. Derleme sırasında Gradle bağımlılıklarını indirmek için internet gerekebilir; kurulu uygulama internet kullanmaz.

## Widget ekleme
Uygulamayı aç → “Ana ekrana widget ekle”; launcher desteklemiyorsa ana ekrana uzun bas → Widget'lar → RaSaSeR Widget Studio. Android widget ayarlayıcıdan eylem, fotoğraf ve görünürlük seç → Kaydet. Uygulama içinden var olan widget'ı düzenleyebilirsiniz.

## Güvenlik ve dürüst sınırlamalar
- Manifest'te INTERNET izni yoktur. Yalnızca isteğe bağlı fener aracı için CAMERA izni kullanıcının işleminde istenir; arama `ACTION_DIAL` kullanır, CALL_PHONE izni yoktur.
- Web bağlantısı haricî tarayıcı açar. Ağ gerektiğinde bu bağlantı çevrimdışı çalışmaz.
- Android launcher widget boyutu/özellikleri ve şeffaf dokunma davranışı cihazlara göre farklılık gösterebilir.
- Uygulama gizlilik modu veya sistem düzeyinde uygulama gizleme aracı değildir.
- APK/AAB henüz derlenmedi veya cihaz testinden geçmedi; kararlı sürüm olduğu iddia edilmez.
- Başkalarının uygulama kodu, markası, görseli veya ekranları kullanılmamıştır.
- Genel kaynak deposu otomatik olarak açık kaynak lisansı vermez. Yayın öncesi lisans ve veri güvenliği beyanı gözden geçirilecek.
- İmzalama ve Play Store release işlemleri PRIVATE ortamda yapılır.

## Kalite kontrol planı
Android 8–17 testleri, widget oluşturma/silme ve telefon yeniden başlatma, yeniden boyutlandırma, fotoğraf izni, farklı üretici launcher'ları, fener izni ve yanlış URL/numara doğrulaması. Bu kontroller henüz cihaz üzerinde doğrulanmadı.
