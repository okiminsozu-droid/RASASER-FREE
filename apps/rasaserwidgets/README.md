# VARYOK — RaSaSeR • APPS & TECHNOLOGY — 0.3.0

Bağımsız Android widget uygulaması. Yalnız `apps/rasaserwidgets/` klasöründe sürdürülür; başka RaSaSeR uygulamalarına, web sitesine veya Cloudflare'a dokunulmaz. Android paket kimliği **com.rasaser.free.rasaserwidgets** korunur.

## Ana ekran
- Yumuşak pembe arka plan, açık pudra renkli kontroller ve yalnızca büyük **Widget Oluştur** düğmesi.
- Sağ üst: bayraklı TR / EN / DE / ES / JA / AR dil menüsü ve ayarlar.
- En altta `www.okiminsozu.com` bağlantısı; cihazın tarayıcısına yönlendirilir.
- Ayarlar'ın **en sonunda** gizlilik ve veri saklama açıklaması tüm altı dilde görünür.

## Widget oluşturma — yeni canlı katalog
- **Widget Oluştur**, bütün widget türlerini ve altında **telefonda açılabilir uygulamaların güncel listesini** tek kaydırılabilir ekranda sunar.
- Uygulamalar isim ve paket kimliğiyle listelenir, arama yapılır; seçilen uygulama widget düzenleyicisinde otomatik hedef ve etiket olarak doldurulur.
- Uygulama listesi açılışta gerektiğinde, ana iş parçacığını engellemeden hazırlanır. Yeni uygulama kurulması, kaldırılması veya güncellenmesi sırasında ekran açıkken sistem bildirimiyle yenilenir; ekran yeniden açıldığında da tekrar sorgulanır. Uygulama envanteri diske yazılmaz.
- Android'in gizlilik ve paket görünürlüğü kuralları nedeniyle **sistem arka plan servisleri veya açılabilir ekranı bulunmayan paketler** listede görünmez. `QUERY_ALL_PACKAGES` izni istenmez.
- Fotoğraflı widget'ta Android Photo Picker (uygunsa) veya sistem dosya seçicisi kullanılır. **Yeni fotoğraflar, galeri seçicisi tekrar açıldığında görünür**; galeri topluca okunmaz, yeni fotoğraflar kullanıcıdan habersiz bir widget'a eklenmez.
- Fotoğrafın boyutlandırılarak uygulamanın özel alanına alınması artık arka planda yapılır, ana arayüz bloke edilmez.
- Diğer widget türleri: şeffaf kısayol (%0–100), web bağlantısı, hazır başlangıç şablonları, telefon arama ekranı, not, Wi-Fi, Bluetooth, sistem ayarları, isteğe bağlı el feneri.
- Her widget ayrı etiket, görsel, opaklık ve işlem taşır. Mevcut widget'lar Ayarlar > Widget'larım içinden düzenlenir.

## Gizlilik (ayarlarda gösterilen açıklamanın özü)
**Kişisel veriler sunucularımıza gönderilmez. Üyelik, reklam takibi ve analitik yoktur.** Fakat widget'ların çalışması için kullanıcı tarafından seçilen görseller, widget etiketleri, hedefleri, görünürlük değerleri ve üç ücretsiz widget sayacı cihaz içinde yerel olarak saklanır. Bunlar için bir sunucu hesabı veya ağ servisi kullanılmaz. INTERNET izni yoktur. Web bağlantısı kullanıcının haricî tarayıcısında açılır. İsteğe bağlı fener için kamera izni gerekebilir.

## Ücretsiz ve Premium modeli
İlk **üç başarılı widget oluşturma** ücretsizdir. Aynı widget'ı düzenlemek yeni hak tüketmez; silinen widget hak iadesi sağlamaz. Dördüncü deneme bir Premium açıklamasıyla engellenir. **Google Play Billing henüz eklenmediği için ödeme işlemi aktif değildir.** Yayın öncesi gerçek satın alma, işlem doğrulama ve satın alma geri yükleme sistemi tamamlanmalıdır.

## Performans ve kalite sınırları
- Uygulama verileri yereldir; yeni uygulama listesini taramak için ağ, arka plan servisi veya kalıcı uygulama envanteri kullanılmaz.
- Büyük fotoğraflar küçültülerek alınır ve asenkron olarak işlenir.
- Aynı ana ekranın açılışta iki kez çizilmesi önlendi.
- Kaynakta regresyon ve gizlilik sözleşmesi kontrolleri vardır.

**Henüz fiziksel cihazda derleme / performans / stabilite doğrulaması yapılmadı.** GitHub Actions derlemesi kullanıcı onayı olmadan başlatılmayacak. Android Studio ile bu klasör ayrı proje olarak açılır; JDK 17 / Android SDK 35.

## Uygulama ekleme sınırları
Android cihazın farklı kullanıcı/iş profillerindeki uygulamalar, başlangıç ekranı bulunmayan sistem servisleri ve bazı üreticiye özel gizlenmiş uygulamalar Android izin kuralları sebebiyle listelenmeyebilir. "Bütün uygulamalar" ifadesi cihazın izin verdiği **başlatılabilir uygulamalar** anlamına gelir.
