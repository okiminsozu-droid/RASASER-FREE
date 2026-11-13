# MİZAÇ — Kişilik Analizi

**RaSaSeR APPS & TECHNOLOGY** | Bağımsız Android uygulaması

- Konum: `apps/mizac/`
- Paket: `com.rasaser.free.mizac`
- İlk sürüm: `0.1.0` (`versionCode=1`)
- Teknoloji: Android / Kotlin / Jetpack Compose
- Hedef: Android 15 (API 35), en düşük Android 8.0 (API 26)
- Çalışma: Offline-first, hesap yok, internet izni yok, kullanıcı verisi aktarımı yok
- Kimlik: Kullanıcının onayladığı **siyah zemin / altın sembol / MİZAÇ / RaSaSeR APPS & TECHNOLOGY** logosu korunur.

## v0.1 kapsamı
1. Ana ekran, logolu açılış ve RaSaSeR marka kimliği.
2. Beş genel kişilik alanına ilişkin 10 soruluk **özfarkındalık örneği**. Bu anket bilimsel olarak valide edilmemiştir; kişilik testi veya tanı gibi sunulmaz.
3. **Mârifetnâme / İlm-i Kıyâfet** bilgi ekranı (tarihî görüşler, güncel bilimden açıkça ayrılmıştır).
4. Gizlilik, kaynak/lisans bilgilendirmesi.

## Kullanıcı verilerinin mülkiyeti ve gizliliği

**Veriler kullanıcıya aittir.** MİZAÇ hizmetinin amacı kişisel verileri toplamak değildir.

- İsteğe bağlı fotoğraflar ve **en fazla 30 saniyelik videolar**, telefonun uygulamaya özel alanına kopyalanır.
- 10 / 50 / 120 soru tercihi yerel SharedPreferences'ta tutulur.
- Mevcut 10 soruluk prototip yanıtları yalnızca geçici ekran durumundadır; kalıcı kişilik geçmişi henüz yazılmaz.
- RaSaSeR şirketi kullanıcının fotoğraflarını, videolarını, cevaplarını veya sonuçlarını kendi sunucularına almaz, analiz için dış servise göndermez.
- Hesap, analitik SDK, reklam SDK'sı, uygulama içi internet istemcisi veya uzaktan kayıt altyapısı kullanılmaz.
- Gizlilik ekranında **Tüm Yerel MİZAÇ Verilerimi Sil** onayıyla yerel uygulama kopyaları ve tercihleri kaldırılır; telefon galerisindeki orijinal dosyalara dokunulmaz.
- Android yedekleme kapalıdır (`allowBackup=false`); üçüncü taraf kamera veya galeri uygulamalarının kendi davranışları kullanıcıya açıkça belirtilir.
- Dış web sitesi bağlantısı kullanıcı dokunmasıyla açılır; uygulama kişilik verilerini web sitesine göndermez.
- Gelecekte yerel analiz geçmişi eklendiğinde aynı cihazda saklama ve tam silme kuralı korunacak. Ağ üzerinden analiz motoruna gönderim izinsiz eklenmeyecek.

## İlkeler
Yüz/boy/beden görünümünden karakter, dürüstlük, zekâ veya niyet çıkarımı yapılmaz. Bu tür fizyonomi anlatıları yalnız tarihî içerik olarak gösterilir. Şeffaflık, rıza ve çevrimdışı kullanım önceliklidir. Klinik teşhis, işe alım kararı veya yüksek riskli karar desteği amaçlanmaz.

## Derleme
Depo kök dizininden: `./gradlew` her uygulamada mevcut olmayabilir. Depo Actions akışı veya uygun JDK17/Android SDK/Gradle ile:
`cd apps/mizac && gradle :app:assembleDebug`
Tüm imzalı release/AAB, güvenli PRIVATE ortamda oluşturulur. PUBLIC depoya anahtar/şifre/token konmaz.

Bu klasör dışında hiçbir uygulama kodu değiştirilmez.
