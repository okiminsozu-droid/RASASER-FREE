# VARYOK — RaSaSeR • APPS & TECHNOLOGY — v0.2.0

Bağımsız Android widget uygulaması; mevcut `apps/rasaserwidgets/` klasöründe geliştirilmeye devam eder. Paket kimliği `com.rasaser.free.rasaserwidgets` ve var olan widget motoru korunur. Diğer RaSaSeR uygulamalarına dokunulmaz.

## Ana ekran (yeni)
- Pembe arka plan, açık pudra vurgular ve yalnızca **Widget Oluştur** ana butonu.
- Widget Oluştur'a dokunulduğunda liste: Fotoğraflı Widget, Şeffaf Kısayol, Uygulama Kısayolu, Web Kısayolu, Hazır Şablonlar (başlangıç önayarları), Telefon Arama, Kişisel Not, Wi-Fi, Bluetooth, Sistem Ayarları, El Feneri.
- Sağ üstte ayarlar simgesi ve bayraklı dil seçimi. Dil seçenekleri: Türkçe, İngilizce, Almanca, İspanyolca, Japonca, Arapça. Ana sayfa, widget düzenleyici ve yerel not/fener ekranı metinleri uygulama içinden değişir.
- Ayarlar → Widget'larım, dil, gizlilik, hakkında. Mevcut widget'lar ana ekranda kalabalık oluşturmaz; **Widget'larım** içinden düzenlenir.
- Alt kısımda dokunulabilir `www.okiminsozu.com` bağlantısı, harici tarayıcıda açılır.
- Üç adet **başarıyla kaydedilen yeni widget** ücretsiz. Mevcut widget'ı düzenlemek ya da sistem tarafından ID geri yüklenmesi yeni hak harcamaz; silmek hak iadesi sağlamaz.
- Dördüncü widget için Premium bilgilendirmesi gösterilir ve ekleme engellenir. **Google Play Billing henüz entegre edilmediğinden satın alma yapılamaz.** Gerçek ücretli dağıtım öncesi fiyat/ürün tanımı, satın alma onayı, işlem doğrulama ve geri yükleme zorunludur. Premium satışı çalışıyor gibi gösterilmez.

## Korunan özellikler
Fotoğraf seçme ve yalnız uygulama içinde saklama, 0–100 görünürlük, her widget için farklı etiket ve tıklama eylemi, uygulama/web/arama/not/Wi-Fi/Bluetooth/ayarlar/fener, widget yeniden boyutlandırma. `none` türü yalnız fotoğraflı, dokunma eylemi olmayan widget oluşturur.

İnternet, depolama, takip, reklam veya analitik izni/SDK yok. Yalnız isteğe bağlı fener özelliğinde kamera izni istenir. Web kısayolu kullanıcının haricî tarayıcısında açılır.

## Kurulum ve QA
Android Studio ile `apps/rasaserwidgets/` ayrı proje olarak açılır. JDK 17 / Android SDK 35 / Kotlin 2.0.21. Mevcut GitHub Actions derlemesi yalnızca kullanıcı onayından sonra çalıştırılabilir. Kaynak dosyalarının GitHub'a yüklenmiş olması **APK/AAB'nin derlendiği veya cihaz üzerinde test edildiği anlamına gelmez**.

Yayın öncesi: Android cihazlarda dil geçişi, 3 ücretsiz hak, oluşturma/silme/düzenleme, Android launcher sabitleme, ekran döndürme, ödeme altyapısı ve Play Store politikaları ayrıca doğrulanmalıdır.
