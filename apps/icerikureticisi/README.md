# RaSaSeR İçerik Üreticisi — Android 0.1.0

Bağımsız, tamamen cihaz üzerinde çalışan içerik üreticisi ve sosyal medya kısayol merkezi.

## Özellikler
- Krem, koyu beyaz ve sıcak gri temalı Jetpack Compose arayüzü.
- Ana sayfada **İçerik Üreticisi +**; sınırsız içerik profili. Profiller adlarıyla listelenir.
- İçerik profilinde Instagram, Facebook, YouTube, TikTok kısayolları ve **+ Sosyal medya hesabı ekle**.
- Her içeriğe sınırsız sosyal hesap bağlantısı; aynı platformdan birden fazla hesap adı eklenebilir.
- Ek platform: sosyal medya adı, hesap etiketi, HTTPS profil adresi ve isteğe bağlı Android paket adı.
- Yüklü resmi uygulamanın orijinal ikonu (cihazda görünürse); değilse marka adı ve tanınabilir simge.
- **Aç** ile kayıtlı HTTPS profil bağlantısının resmi uygulamada açılması; yoksa tarayıcıya yönlendirme. Uygulama oturumlarını kendi içine aktarmaz.
- Android NotificationListenerService ile kullanıcı izninden sonra, ekli uygulamaların **etkin** bildirim sayıları ve bildirim önizlemesi.
- Kaydedilen içerik adları ve profil bağlantıları cihazdaki SQLite'ta. Giriş bilgisi, parola, çerez, API tokenı tutulmaz.

## Android'in sınırları
1. Başka bir uygulamanın oturumuna, mesaj kutusuna, bildirim geçmişine veya hesabına izin verilmeden doğrudan erişilemez. Uygulama içinde OAuth veya parola isteyen sahte giriş ekranı **yoktur**.
2. Android bildirimleri paket/uygulama bazında verir. **Bir uygulamadaki farklı sosyal hesapların hangisine ait olduğu güvenilir şekilde ayrılamaz.** Aynı Instagram uygulaması eklenmiş birden fazla içerik profiline bağlanırsa görünen sayı uygulama düzeyindeki ortak etkin bildirim sayısıdır; ayrı hesapların okunmamış mesaj sayısı değildir.
3. Yalnız hâlen etkin Android bildirimleri RAM'de tutulur. Silinen/kapalı bildirimler ve platform içi okunmamış mesaj sayısı geri getirilemez. İlgili uygulamanın kendi bildirim ayarları da açık olmalıdır.
4. Bildirim izni Android sistem ayarlarından **elle** verilir ve istenildiğinde geri alınabilir. İşletim sistemi bu özel erişimde bütün uygulama bildirimlerine teknik olarak erişim tanıyabilir; kod yalnız kaydedilen paketlerin bildirimlerini işler.
5. Sosyal medya uygulaması yoksa HTTPS profil linki uygun tarayıcıda açılır. Bir hesap adına kaydedilen bağlantı o hesap için otomatik giriş sağlamaz; oturum değiştirme ilgili sosyal uygulamada yapılır.
6. İstendiği kadar profil/hesap eklenebilir; gerçek üst sınır cihaz belleği ve depolamasıdır.

## Gizlilik / güvenlik
- **Android INTERNET izni yok.** Doğrudan ağ çağrısı, analiz/izleme SDK'sı, reklam SDK'sı, bulut senkronizasyonu yok.
- Bildirim başlık/metinleri kalıcı kayda veya sunucuya gönderilmez.
- SQLite veritabanı yalnız uygulama dahili alanında; Android yedeği kapalıdır.
- Bildirim erişimi ve özel sosyal hesap bağlantıları yalnız cihazda kullanıcı onayıyla kullanılır.
- PUBLIC GitHub'a kullanıcı giriş bilgileri, keystore veya Play imzalama sertifikası eklenmez.
- Paket listesinde geniş kapsamlı QUERY_ALL_PACKAGES izni bulunmaz.
- Başka uygulama üzerinden profil açma yalnız HTTPS bağlantıları üzerinden yapılır.
- Üretim öncesi üçüncü taraf logo ve lisans kullanımları ayrıca gözden geçirilmelidir.

## Derleme (özel imza gerektirmez)
Depo kökünden Android SDK/JDK 17 ve Gradle kurulu ortamda:
```sh
cd apps/icerikureticisi
gradle :app:assembleDebug :app:testDebugUnitTest
```
Repository Actions şablonu destekliyorsa `app_slug=icerikureticisi` seçilerek **onaydan sonra** Actions başlatılabilir. Bu kod eklenirken workflow tetiklenmedi.

## Durum
Kaynak kodu / ilk prototip; **cihaz üzerinde test edilmiş veya imzalı yayın sürümü değildir**. Paket: `com.rasaser.free.icerikureticisi`.
Diğer RaSaSeR projelerine ve özel motor koduna dokunulmaz.
