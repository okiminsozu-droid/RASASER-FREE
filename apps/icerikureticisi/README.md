# RaSaSeR İçerik Üreticisi — Medya ve Zamanlama 0.2.0

Bu Android uygulaması telefonunuzdaki sosyal medya içerik profillerini, fotoğraf/video taslaklarını ve paylaşım hatırlatmalarını düzenler. Krem tema, profiller, kayıtlı hesaplar ve yerel gizlilik ilkesi korunur.

## Yeni özellikler
- **Sosyal medya uygulamasını doğrudan açma:** Kayıtlı platformun paketinde uygun dışa açık görüntüleme ekranı tespit edilirse açıkça belirtilmiş Android component'i açılır. Böylece aynı uygulamanın normal/kopya sürümleri arasındaki sistem seçicisi mümkün olduğunda atlanır; o platformun resmî uygulamasının mevcut oturumu kullanılır. Uygulama yüklü değilse HTTPS profil bağlantısına geçilir.
- **Resim/video seçimi:** Android'in sistem belge seçicisi ile sadece kullanıcının seçtiği içerik alınır. Tüm galeriye erişim izni istenmez. Zamanlanmış taslaklarda URI için Android'in kalıcı okuma izni kullanılır.
- **Açıklama:** Seçilen medya altında paylaşım metni yazılır; hedef uygulamaya `EXTRA_TEXT` ile gönderilir, bazı uygulamalar medya paylaşımı sırasında bunu almadığından ayrıca cihaz panosuna kopyalanır.
- **Sınırsız hesap kaydı ve tekli/toplu seçim:** Her hesap kartında kutucuk bulunur; paylaşım panelinde **Hepsini seç**, **➤ Gönder**, **◷ Zaman** kontrolleri yer alır.
- **Gönder:** İlk seçilen hesabın yüklü resmî uygulamasına Android'in `ACTION_SEND`/URI izinleri ile medya aktarılır. Birden fazla seçildiyse uygulamaya geri dönünce **Sıradaki hesaba aktar** düğmesiyle hedefler sırayla açılır. Her platformda son yayın onayı ve gerekirse doğru hesap seçimi kullanıcı tarafından yapılır.
- **Zamanlama:** İleride bir tarih/saat seçilir. Android WorkManager yaklaşık zamanda **yerel bildirim** oluşturur; bildirimden uygulama ve zamanlanmış taslak açılır, seçilen hesaplara paylaşım hazırlığı yapılır. Zamanlanmış taslaklar cihazda SQLite ile saklanır, silinebilir ve yeniden açılabilir. **Kendiliğinden sosyal medya hesabına yüklemez.**
- **İzinler:** Android 13 ve üzerinde ilk açılışta yalnız zamanlanmış paylaşım hatırlatmaları için bildirim iznini açıklayan tek bir isteğe bağlı ekran gösterilir. Kullanıcı Android izin penceresinden onaylar veya reddeder; izin hiçbir zaman otomatik onaylanamaz. Sosyal medya bildirimlerini okumaya yönelik hassas NotificationListenerService izni bu sürümde yoktur.
- **Arayüz:** Eski "Güvenli kurulum sürümü" bilgi kartı çıkarıldı; yerine medya oluşturma kartı yerleştirildi. Liste uzun olduğunda bütün profil sayfası dikey kaydırılır.

## Resmî platform sınırlamaları
Bu sürüm kullanıcının **mevcut telefon uygulamasına paylaşım akışı** oluşturur. Instagram, Facebook, YouTube ve TikTok'a her türlü hesabı tek dokunuşla/arka planda otomatik yayınlayan bir servisin var olduğu iddia edilmez. OAuth yetkileri, geliştirici uygulama onayları, işletme/creator hesabı koşulları ve platforma özel yükleme API'leri ayrı ayrı sağlanmadan gerçek otomatik yayınlama yasal ve teknik olarak uygulanamaz. Kodda sosyal medya parolası, access token, gizli anahtar veya sunucu API'si yoktur.

Bildirim izni yalnız **kendi planlı paylaşım hatırlatmalarımız** içindir; başkalarının bildirimlerini, sosyal medya mesajlarını veya okunmamış sayaçlarını göstermez.

## Kurulum ve mevcut kayıtların korunması
**Önemli:** 0.1.1'in public GitHub Actions debug imza anahtarı geçici olduğundan, sonraki debug APK aynı Android paketini güncellemek yerine `com.rasaser.free.icerikureticisi.preview` kimliğiyle ayrı **İçerik Üreticisi Test** uygulaması olarak üretilir. Böylece telefondaki 0.1.1 ve içindeki hesaplar **silinmez**. Test uygulamasına hesapları yeniden tanımlamak gerekir. Test sürümü başarılı olduktan sonra kalıcı, korunmuş bir imza anahtarıyla yayın süreci planlanmalıdır; public GitHub'a keystore veya şifre konmaz.

- Kaynak applicationId: `com.rasaser.free.icerikureticisi`
- GitHub debug/önizleme applicationId: `com.rasaser.free.icerikureticisi.preview`
- Sürüm: 0.2.0 (3), Android minSdk 26; internet izni yok
- Zamanlama WorkManager'a bağlıdır, Android batarya optimizasyonu nedeniyle saniye hassasiyetinde garanti edilmez. Cihaz kapalıyken veya uygulama zorla durdurulduğunda hatırlatma gecikebilir.
- Kullanıcı seçtiği belgeyi taşır/silerse kalıcı URI erişimi sona erebilir; taslak yeniden medya seçimi gerektirir.
- Android clipboard ve içerik paylaşımı cihazın diğer uygulamalarıyla kullanıcı yönlendirmesi üzerinden çalışır. İçerik sunucularımıza yüklenmez.

## Derleme ve güvenlik
`gradle --no-daemon --project-dir apps/icerikureticisi :app:assembleDebug :app:testDebugUnitTest`

GitHub Actions `.github/workflows/04-icerik-ureticisi-debug.yml` üzerinden yalnız güvenli debug APK üretir. Bildirim dinleyici servisinin bulunmadığı ve test paketinin `.preview` olduğu derleme sonrası denetlenir. Üretim Play AAB, OAuth entegrasyonu ve gerçek cihaz otomasyonu henüz hazır değildir.

Kaynaklar:
- https://developer.android.com/develop/ui/compose/sharing/send
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work
- https://developers.tiktok.com/products/content-posting-api/
- https://developers.google.com/youtube/data/guides/authentication

Diğer RaSaSeR projelerine ve PRIVATE motorlara dokunulmaz.
