# CAR LAUNCHER — ilk arayüz testi

Kullanıcı ZIP dosyasında bulunan 10 Android Studio dosyası içerikleri **değiştirilmeden** korunmaktadır.

- ZIP SHA-256: `8b8b56be6b396f755181ff1d0b21b3ffe1e5ca75b7e09d6978ea6ba3a448d650`
- Kaynak dosyaların serileştirilmiş SHA-256: `52f901cdcf37427f6b6e4bcdf7b4a1509ae9cedd7d8e97badebf7ca325374b4d`
- Depolama: `apps/carlauncher/source-packed/part1.txt`–`part6.txt` (orijinal kaynaklar Brotli ile kayıpsız sıkıştırılmış, Base64 parçaları)
- Açma: `python3 apps/carlauncher/unpack_original.py` (CI Brotli paketini sağlar)
- GitHub Actions: `CAR LAUNCHER - Original ZIP Debug APK`
- Android Gradle Plugin 8.5.2, Gradle 8.7, Kotlin 1.9.24, Android API 34; hepsi ZIP'teki sürümler
- Yeni imzalı sürüm veya Play Store paketi değildir. PRIVATE motor bağlanmadı.
- Test paketi, yalnız özgün ZIP içindeki arayüzü görmek için debug APK olarak hazırlanır.

Arşiv kaynaklarını değiştirmeden aynen oluşturur; Android SDK ve Gradle yürütücüsü dış derleme ortamından sağlanır. Test derleme hata verirse Kotlin kodu izin olmadan değiştirilmez.
