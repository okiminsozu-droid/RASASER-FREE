# RaSaSeR İçerik Üreticisi — 0.3.0 Özellik Testi

**Kapsam:** Yalnızca `apps/icerikureticisi/`, ayrı `feature/icerik-ureticisi-brand-secure-multilang-v030` dalı. Çalışan 0.2.0 sürümü ve önceki tüm Android projeleri değiştirilmez.

## Yeni eklenenler
- İçerik üreticisi profili oluştururken galeriden fotoğraf/logo; var olan profile görsel değiştirme. Görsel SQLite'ta yalnız dosya yolu ve `filesDir` içinde özel küçük PNG olarak tutulur. Liste kartındaki varsayılan numara yerine profil görseli görünür. SQLite v3→v4 veri silmeyen migration.
- 🇹🇷 Türkçe, 🇬🇧 İngilizce, 🇩🇪 Almanca, 🇪🇸 İspanyolca, 🇯🇵 Japonca, 🇸🇦 Arapça dil açılır menüsü; bilinen arayüz etiketleri yerel çeviri tablosundan değişir. Kullanıcının yazdığı profil/hesap adları yanlış otomatik çevrilmez; profiller için 🌐 menüsünden her dile isteğe bağlı karşılık girilebilir. Bilinmeyen dinamik metin kaynak dilde kalabilir.
- Krem/açık tema ve antrasit/koyu tema seçimi; cihazda kalıcı tercih.
- Sol üst Premium simgesi, Google Play'de RaSaSeR araması ve ortak RaSaSeR promosyon kodu doğrulama alanı. Google Play satış/abonelik entegrasyonu **yoktur**, mağazada yayımlanmamış bir uygulama için uydurma ürün bağlantısı kullanılmaz.
- Premium kodu APK'ya veya GitHub'a konmaz. Halihazırdaki Cloudflare `/v1/promo/redeem` servisi üzerinden **HTTPS ve sunucuda D1 whitelist** kontrolü; `owner_lifetime` yanıtı alınırsa cihazdaki hak Android Keystore HMAC ile mühürlenir. İlk etkinleştirme internet gerektirir. Bu paketin Cloudflare izin listesinde yetkili olması ayrıca gereklidir; aksi durumda başarısız doğrulama, Premium hakkı vermez.
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
