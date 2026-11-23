# RaSaSeR İçerik Üreticisi — 0.3.2 Paylaşım ve Premium Geliştirme

**Kapsam:** Yalnızca `apps/icerikureticisi/`, ayrı `feature/icerik-ureticisi-biometric-trial-cloudflare-v031` dalı. Çalışan 0.2.0 sürümü ve önceki tüm Android projeleri değiştirilmez.

## 0.3.2 düzeltmeleri (23 Kasım 2026)
- İçerik profili açılınca kaydedilmiş hesaplar **varsayılan olarak seçili**; kullanıcı istediği hesapların işaretini kaldırabilir. Yeni eklenen hesap da otomatik işaretlenir.
- Kaydedilen uygulama için **doğrudan başlatma intenti** öncelenir. Mevcut `ACTION_SEND` medya paylaşımı uygun uygulamaya açık bileşen üzerinden aktarılır. **Telefonun uygulama klonlama sistemi** (örneğin Vivo'nun `Ⅱ·Instagram` seçicisi) farklı kullanıcı profilleri açtığından Android'in sistem seçim penceresi her durumda otomatik atlanamayabilir.
- Paylaşım uygulaması açılınca üzerine **“Bilgi” uyarısı bindirilmez**. Uygulamaya aktarılan içerik için yerel durum görüntülenir. Kullanıcı gerçek yayını platform uygulamasında tamamladıktan sonra **“Gönderildi olarak işaretle”** diyebilir. Bu, platform onayı değil **kullanıcının manuel teyididir**; sahte başarı bildirimi gösterilmez.
- Profil bağlantısı kaydetmek, otomatik paylaşım yetkisi sağlamak anlamına gelmez. Instagram/Facebook/YouTube/TikTok'ta gerçek hesap bazlı yayın ve platform onaylı gönderim bildirimi **ancak resmi OAuth, yayın API yetkileri, developer uygulaması ve uygun platform incelemeleriyle** mümkündür. Bu sürümde otomatik API yayını yapılmaz.
- Premium üst ikonu standart emoji yerine **özgün siyah-altın vektör taç** tasarımına çevrildi. Premium hakkı varsa aktif durumu gösterir.
- 15 günlük deneme dolunca uygulama girişinde **Premium ekranı doğrudan açılır**; yeni Premium etkinleştirmesi ancak Cloudflare'de doğrulanan promosyon koduyla mümkündür. İçerikler silinmez. Doğrulama sonrasında ekran yenilenerek tam erişim açılır.
- **Google Play ücretli satın alma**: Satış için bu uygulamanın Play Console'a eklenmesi, ürün/abonelik ve resmi Billing Library + güvenli sunucu satın alma onayı gerekir. Henüz yapılandırılmadığından satın alma düğmesi gibi yanıltıcı bir kontrol gösterilmez.
- Yeni kaynak sürüm **0.3.2 (versionCode 6)**. Debug paket kimliği `com.rasaser.free.icerikureticisi.v030` olarak tutulur; farklı test imzaları yüzünden yükseltme zorunlu olarak başarılı olmayabilir. Önceki kurulum kaldırılmamalıdır.

## 0.3.1 düzenlemeleri (23 Kasım 2026)
- **Telefonun yerel parmak izi:** PIN oluşturulunca biyometrik geçiş belirginleşir; parmak izini etkinleştirmek için Android BiometricPrompt üzerinden gerçekten başarılı cihaz doğrulaması istenir. PIN güvenli yedek olarak kalır. Parmak izi görüntüsü/şablonu uygulama tarafından alınmaz veya saklanmaz.
- **Üst ve alt hizalama:** Üst kontrol ikonları ekran genişliğine dengeli yerleştirilir; Android güvenli ekran alanları (durum çubuğu, gezinme çubuğu, çentik) dikkate alınır. Alt marka ve site yazısı telefon gezinme çubuğunun üstünde ortalanır.
- **15 gün ücretsiz deneme:** İlk açılışta başlayan çevrimdışı tam erişim. Keystore HMAC ile mühürlenen yerel başlangıç/son görüntüleme saati tutulur. Saati geriye alarak uzatmaya karşı temel koruma vardır. Süre bitince işlemler Premium ekranı açılana kadar engellenir; yerel içerikler otomatik silinmez. Çevrimdışı cihazdaki deneme takibi tam anlamıyla manipülasyona dayanıklı lisans sunucusu değildir.
- **Promosyon kodu / Cloudflare:** Mevcut, kullanıcı verilerinden bağımsız `rasaser-bagimsiz-kullanici-girisi` Worker ve D1 `owner_allowed_packages` izin listesine hem `com.rasaser.free.icerikureticisi` hem `com.rasaser.free.icerikureticisi.v030` paketleri yetkilendirilmiştir. Kod APK'ya, GitHub'a veya uygulama tercih dosyasına yazılmaz; ilk etkinleştirme için internet gerekir. Kod yalnızca sunucudan doğrulanır.
- **Sürüm:** source 0.3.1 / versionCode 5. Yan yana kurulan debug paket kimliği `.v030` sabit bırakılmıştır; mevcut test verilerini koruyan güncelleme yolu amaçlanır.
- **Henüz yapılmayanlar:** Google Play Billing ürün, abonelik ve satın alma altyapısı yoktur. "Google Play'de aç" yalnız RaSaSeR araması açar; satın alma veya deneme sonrası otomatik tahsilat yapmaz. Üretim yayınına geçmeden önce gerçek bir satın alma yolu ve mağaza uyumluluğu hazırlanmalıdır. Android cihazda biyometrik/promosyon uçtan uca test ve APK derlemesi henüz çalıştırılmamıştır. GitHub Actions maliyetine sebep olmamak için otomatik derleme yeni dalda tetiklenmemiştir.

## Yeni eklenenler
- İçerik üreticisi profili oluştururken galeriden fotoğraf/logo; var olan profile görsel değiştirme. Görsel SQLite'ta yalnız dosya yolu ve `filesDir` içinde özel küçük PNG olarak tutulur. Liste kartındaki varsayılan numara yerine profil görseli görünür. SQLite v3→v4 veri silmeyen migration.
- 🇹🇷 Türkçe, 🇬🇧 İngilizce, 🇩🇪 Almanca, 🇪🇸 İspanyolca, 🇯🇵 Japonca, 🇸🇦 Arapça dil açılır menüsü; bilinen arayüz etiketleri yerel çeviri tablosundan değişir. Kullanıcının yazdığı profil/hesap adları yanlış otomatik çevrilmez; profiller için 🌐 menüsünden her dile isteğe bağlı karşılık girilebilir. Bilinmeyen dinamik metin kaynak dilde kalabilir.
- Krem/açık tema ve antrasit/koyu tema seçimi; cihazda kalıcı tercih.
- Sol üst Premium simgesi, Google Play'de RaSaSeR araması ve ortak RaSaSeR promosyon kodu doğrulama alanı. Google Play satış/abonelik entegrasyonu **yoktur**, mağazada yayımlanmamış bir uygulama için uydurma ürün bağlantısı kullanılmaz.
- Premium kodu APK'ya veya GitHub'a konmaz. Halihazırdaki Cloudflare `/v1/promo/redeem` servisi üzerinden **HTTPS ve sunucuda D1 whitelist** kontrolü; `owner_lifetime` yanıtı alınırsa cihazdaki hak Android Keystore HMAC ile mühürlenir. İlk etkinleştirme internet gerektirir. İzin listesine bu paketin normal ve v030 test kimlikleri 23 Kasım 2026'da eklenmiştir; doğrulama başarısızsa Premium hakkı verilmez.
- Üst çubukta 🔐 PIN oluştur/değiştir + parmak izi aç/kapat. PIN 6–12 rakam, rastgele tuzla PBKDF2-HMAC-SHA256 (180.000 yineleme), beş yanlış denemede 30 sn kilit. Güçlü parmak izi destekli cihazda kullanıcı onayıyla BiometricPrompt; PIN yedeği her zaman var. PIN kurulduktan sonra sonraki uygulama açılışı kilitli başlar; başarıyla kimlik doğrulanınca uygulama otomatik açılır.
- Ana sayfa altında `RaSaSeR APPS & TECHNOLOGY` + tıklanabilir `www.okiminsozu.com`.
- Uygulama ikonunda özgün krem, altın ve siyah klaket/kamera çizimi; tüm ekranlarda ölçeklenebilir vektör kaynak.

## Korunan özellikler
Sınırsız içerik profili/sosyal medya kısayolları, Instagram/Facebook/YouTube/TikTok ve özel platformlar, resmî uygulamayı doğrudan açma, fotoğraf/video seçip açıklama yazma, hesap kutucukları/Hepsini seç, sırayla paylaşım aktarımı, WorkManager ile zamanlanmış **hatırlatma**.

**Önemli:** Zamanlanan medya resmî sosyal medyaya kendi kendine yüklenmez. Yayın onayı kullanıcının ilgili platform uygulamasında verilir. Instagram/Facebook/YouTube/TikTok paylaşımları için platform OAuth/API yetkileri olmadan gerçek, otomatik, hesap bazlı yayın yapılmaz.

## Güvenlik ve uyumluluk
- `android.permission.INTERNET` sadece Premium HTTPS doğrulaması ve tarayıcı/site açılışları için gerekir; profiller, PIN ve medya verileri buluta yüklenmez.
- `USE_BIOMETRIC` Android normal iznidir. Parmak izi kullanımı sistemin açık onay ekranıyla gerçekleşir. Başka uygulamaların bildirimlerini okuyan NotificationListenerService yoktur.
- Galeriden fotoğraf seçimi Android sistem seçicisiyle; geniş depolama izni verilmez.
- İlk açılıştaki yayın hatırlatması bildirim izni Android 13+ üzerinde kullanıcı onayıyla istenir. Sistem izinleri otomatik onaylanamaz.
- PIN sıfırlama güvenlik nedeniyle kolaylaştırılmadı; hem PIN hem biyometrik erişim kaybolursa uygulama verilerini silmeden kurtarma mekanizması yoktur.
- Debug APK `com.rasaser.free.icerikureticisi.v030` paket kimliğiyle mevcut 0.1.1/0.2.0 testlerini silmeden ayrı kurulur. Bu yüzden hesaplar yeni test sürümüne otomatik taşınmaz. Google Play üretim sürümünün imza/yükseltme stratejisi ayrıca gerekir.
- Ortak promosyon kodu sunucuda doğrulanmadan Premium etkin sayılmaz. **Diğer uygulamalara otomatik entegrasyon yapılmaz.** Kod tek olsa bile uygulama bazında API entegrasyonu gerekir.
- Logoda RaSaSeR marka dışı tescilli sosyal platform ikonlarının yetkileri ayrı değerlendirilmelidir.

## Derleme
```bash
gradle --no-daemon --project-dir apps/icerikureticisi :app:assembleDebug :app:testDebugUnitTest
```
Derlemeden önce sürüm ve en az bir kimlik doğrulama testi doğrulanmalıdır. GitHub Actions iş akışı yeni dala özel koşacak, hassas NotificationListenerService bulunmadığını ve debug kimliğinin `.v030` olduğunu kontrol edecek.

Kaynaklar: AndroidX Biometric, Android Keystore, Android per-app language, Google Play Billing promo ve Cloudflare Workers resmî belgeleri.
