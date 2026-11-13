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

## İlkeler
Yüz/boy/beden görünümünden karakter, dürüstlük, zekâ veya niyet çıkarımı yapılmaz. Bu tür fizyonomi anlatıları yalnız tarihî içerik olarak gösterilir. Şeffaflık, rıza ve çevrimdışı kullanım önceliklidir. Klinik teşhis, işe alım kararı veya yüksek riskli karar desteği amaçlanmaz.

## Derleme
Depo kök dizininden: `./gradlew` her uygulamada mevcut olmayabilir. Depo Actions akışı veya uygun JDK17/Android SDK/Gradle ile:
`cd apps/mizac && gradle :app:assembleDebug`
Tüm imzalı release/AAB, güvenli PRIVATE ortamda oluşturulur. PUBLIC depoya anahtar/şifre/token konmaz.

Bu klasör dışında hiçbir uygulama kodu değiştirilmez.
