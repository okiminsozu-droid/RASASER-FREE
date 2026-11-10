# CAR LAUNCHER — RaSaSeR FREE

**Bağımsız uygulama çalışma klasörü:** `apps/carlauncher/`

Kaynak referansı: Kullanıcının sağladığı `RaSaSeR_CarLauncher_AndroidStudio.zip` (10 dosya, orijinal SHA-256: `8b8b56be6b396f755181ff1d0b21b3ffe1e5ca75b7e09d6978ea6ba3a448d650`). Bu arşiv inceleme için alındı; **henüz uygulama kaynak kodları bu klasöre aktarılmadı**. Boş/örnek şablon gerçek Launcher olarak sunulmayacak.

## Amaç
Android 15 araç multimedya cihazları için yatay premium araç ana ekranı. Bağımsız uygulama, mevcut PRIVATE RaSaSeR uygulamalarına ve motorlarına dokunmaz.

## ZIP'te saptanan özellikler
- Android HOME/Launcher seçimi, yatay ekran, saat ve tarih, siyah-altın panel
- GPS hız ve konum, OpenStreetMap harita, Nominatim adres arama, OSRM demo rota
- Open-Meteo hava durumu, radyo oynatıcı, sesli adres arama
- Uygulama çekmecesi, harici uygulama kısayolları, tema/dil seçimi
- Radar, araç bilgisi/OBD, widget ve gerçek AI henüz işlevsel değil

## İş sırası
1. ZIP kaynaklarını bu klasöre gerçek Android Studio proje ağacı olarak güvenli biçimde aktar ve içeriği denetle.
2. Debug imzasıyla "release" üretme yanlışını kaldır; PUBLIC Actions yalnız SECRET içermeyen debug/test üretir.
3. Android 15 cihaz desteğini koruyarak güncel Google Play hedef API gereğini ve Gradle/Kotlin uyumunu sağla.
4. Önce mevcut işlevleri gerçek cihazda test et, eksik modülleri kademeli tamamla.
5. Lisans ve üçüncü taraf atıflarını uygulama içindeki Ayarlar > Lisanslar bölümüne ekle.
6. Motorlu son release ve AAB imzalama PRIVATE ortamda; Play yükleme ve kalıcı imzalı arşiv PRIVATE RaSaSeR'de.

**İlk durum:** Sadece proje klasörü ve çalışma kaydı oluşturuldu; APK/AAB derlenmedi, motor bağlanmadı ve Google Play'e yüklenmedi.

Detaylı teknik rapor: [CAR-LAUNCHER-ZIP-INCELEME.md](../../docs/CAR-LAUNCHER-ZIP-INCELEME.md)
