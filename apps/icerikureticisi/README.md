# RaSaSeR İçerik Üreticisi — Güvenli kurulum sürümü 0.1.1

Android telefonlarda içerik profilleri ve sosyal medya kısayollarını düzenleyen, tamamen cihaz içi çalışan uygulama.

## Özellikler
- Krem arka plan, koyu beyaz butonlar; modern Jetpack Compose.
- Ana sayfada **İçerik Üreticisi +**; sınırsız sayıda isimli içerik profili (pratik sınır cihaz depolamasıdır).
- Her içerik profiline sınırsız sosyal medya bağlantısı ekleme.
- Instagram, Facebook, YouTube, TikTok hazır kısayolları; farklı sosyal medya adı, profil adresi, isteğe bağlı Android paket adı ve galeriden özel logo.
- Cihazda kurulu resmi uygulamayı/HTTPS profil bağlantısını açar, uygulama yoksa tarayıcıyı kullanır.
- İsim, hesap etiketi, profil bağlantısı ve logolar yalnızca telefonda saklanır. Şifre/çerez/token saklamaz.
- Doğrudan INTERNET izni, hesap giriş ekranı, reklam/takip SDK'sı ve bulut senkronizasyonu yok.

## Play Protect uyarısına yönelik düzeltme
**0.1.0** sürümünde Android NotificationListenerService ve `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` servis izni vardı. Google Play Protect bazı cihazlarda tarayıcı/dosya yöneticisi üzerinden indirilen, hassas bildirim erişimi isteyen APK'ların kurulumunu engeller. Uyarının ardından **0.1.1** güvenli kurulum sürümünde:
- Bildirim dinleme servisi, erişim izni ve ilgili uygulama içi izin yönlendirmeleri **tamamen kaldırıldı**.
- Kullanıcıdan telefonun tüm bildirimlerini okuyabilen özel erişim istenmez.
- Bu sürümde sosyal medyanın bildirim sayılarını ve metinlerini **göstermez**; bildirimler ilgili resmi sosyal medya uygulamalarında görülür.
- Önceki bildirimli prototip kaynak kodu `archive/icerik-ureticisi-bildirimli-v010` dalında ayrı tutulur. Bildirim özelliğinin ileride Google Play üzerinden dağıtımı ayrıca incelenecektir.

**Önemli:** Bildirim servisini kaldırmak Play Protect uyarı ihtimalini azaltır; Google'ın diğer güvenlik kontrollerine karşı **kurulum garantisi vermez**. Play Protect'i kapatmayı önermiyoruz.

## Derleme / test
- Gradle 8.9, JDK 17, Android SDK 35; `gradle :app:assembleDebug :app:testDebugUnitTest`
- GitHub Actions `.github/workflows/04-icerik-ureticisi-debug.yml` özel anahtarsız debug APK üretir, oluşturulan APK'nın son AndroidManifest'inde notification listener servisi bulunmadığını da kontrol eder.
- Debug imzası sadece cihaz testi içindir; Google Play yayın AAB'si değildir. Telefon üzerinde gerçek kurulum/işlev testleri kullanıcının cihazında tamamlanmalıdır.

## Gizlilik
- Paket: `com.rasaser.free.icerikureticisi`, sürüm: `0.1.1` (`versionCode 2`).
- Android yedeği devre dışı. İmza anahtarı, kullanıcı giriş bilgisi, sosyal medya hesabı parolası veya API tokenı GitHub'a kaydedilmez.
- Yalnızca `apps/icerikureticisi/` ve ona özel test workflow'u değiştirilir; diğer RaSaSeR uygulamaları ve PRIVATE motor korunur.
- Üçüncü taraf marka/logoların kullanım koşulları yayın öncesinde kontrol edilmelidir.

Referans: https://developers.google.com/android/play-protect/warning-dev-guidance
