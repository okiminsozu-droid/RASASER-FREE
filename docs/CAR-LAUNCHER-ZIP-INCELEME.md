# RaSaSeR Car Launcher — Kaynak ZIP incelemesi

İnceleme tarihi: 10 Kasım 2026  
Kaynak: Kullanıcının yüklediği `RaSaSeR_CarLauncher_AndroidStudio.zip`  
SHA-256: `8b8b56be6b396f755181ff1d0b21b3ffe1e5ca75b7e09d6978ea6ba3a448d650`  
Dosya: 10 adet; açılmış toplam 40.114 bayt; ana Kotlin dosyası yaklaşık 685 satır.

**Durum:** Bu belge kaynak kodun statik incelemesidir. ZIP, FREE deposuna henüz Android uygulama kaynakları olarak aktarılmadı; APK/AAB oluşturulmadı ve cihaz testi yapılmadı. Mevcut PRIVATE projeye dokunulmaz.

## ZIP projesi

- Android Gradle Plugin 8.5.2, Kotlin 1.9.24, Gradle wrapper properties 8.7
- Kotlin + Jetpack Compose + Material 3; osmdroid 6.1.18
- `applicationId = com.rasaser.launcher`; `minSdk=26`, `compileSdk=34`, `targetSdk=34`, v1.0 (1)
- Yatay ekran zorlanıyor; `HOME` ve `LAUNCHER` manifest kayıtları mevcut
- Wrapper properties var, ancak `gradlew`, `gradlew.bat` ve `gradle-wrapper.jar` yok.
- Mevcut release yapılandırması **debug imzası kullanıyor**; asla Play yayınına uygun kabul edilmez.

## Özellik envanteri (kodda gözlenen gerçek düzey)

| Modül | ZIP içeriği | Durum |
|---|---|---|
| Launcher ana ekranı | HOME/DEFAULT + LAUNCHER intent-filter | Temel entegrasyon mevcut; gerçek cihazda test edilmeli |
| Yatay tam ekran | Landscape + sistem çubuklarını gizleme | Kod mevcut |
| Siyah/altın tema | Kart, çerçeve, durum, hız göstergesi | Kod mevcut |
| Tema seçimi | Altın / mavi / yeşil vurgu döngüsü | Çalışma mantığı var; kalıcı kayıt yok |
| Saat/tarih | 1 saniyede güncelleme | Kod mevcut |
| Dil bayrakları | TR, EN, DE, ES, JA | Yalnız TR ve EN sözlükleri var; diğerleri İngilizce yedek |
| GPS konumu | Konum izni, GPS ve ağ konumu dinleyicisi | Kod mevcut; yaklaşık konum, hata ve izin akışı eksik |
| GPS hız | Location.speed × 3.6, km/sa | Kod mevcut; GPS verisi bayatladığında sıfırlama yok |
| Harita | osmdroid / OSM Mapnik, zoom, merkezleme, marker | İnternet/önbellek bağımlı |
| Koyu harita | Tile renk tersine çevirme | Kod mevcut, harita stil kalitesi/testi gerekli |
| Adres arama | Nominatim API sorgusu, tek sonuç | Temel kod mevcut; kota ve sağlayıcı uyumluluğu kritik |
| Rota oluşturma | OSRM demo API, çizgi overlay'i, km/dk | Temel kod mevcut; adım adım veya sesli yol tarifi yok |
| Sesle adres arama | Android RecognizerIntent | Mevcut; gerçek yapay zekâ asistanı değil |
| Müzik/radyo | Android MediaPlayer çevrimiçi yayın | Oynat/duraklat kodu var; stream dini müzik değil, örnek SomaFM |
| Önceki/sonraki parça | Görsel ⏮ / ⏭ simgeleri | Eylem bağlanmamış |
| Hava durumu | Open-Meteo sıcaklık, nem, rüzgâr, kod | 15 dakikada yenileme; sağlayıcı lisans/atfı kontrol edilecek |
| Bağlantı durumu | Ağ ve Bluetooth göstergesi | Ağ temel kontrolü var; BT izni/uyumluluğu eksik |
| Radar kartı | “Aktif Değil” ve resmî kaynak yazısı | **Canlı radar servisi yok**; “resmî kaynak” doğrulanmış canlı veri değil |
| Uygulama çekmecesi | Kurulu launcher uygulamalarının simgeli listesi | Kod mevcut; Android paket görünürlüğü test edilmeli |
| Kısayollar | Google Maps, YouTube, Spotify, Chrome | Yüklüyse başlatır; özel düzen/kayıt yok |
| Telefon | ACTION_DIAL | Harici telefon uygulaması başlatma denemesi |
| Dosyalar | DownloadManager.ACTION_VIEW_DOWNLOADS | Dosya yöneticisi değil, indirme ekranı |
| Kamera | MediaStore kamera intent'i | Harici kameraya yönlendirir |
| Ses kayıt | RECORD_SOUND_ACTION | Harici kaydediciye yönlendirir |
| Ekran yansıtma | Android cast ayar intent'i | Sistem desteğine bağlı |
| Bluetooth | Android Bluetooth ayarları | Sistem ayarına yönlendirir |
| Araç bilgisi | “Yakında eklenecek” toast | Yer tutucu |
| OBD | “Yakında eklenecek” toast | Yer tutucu |
| Widget | “Yakında eklenecek” toast | Yer tutucu |
| Kapat | moveTaskToBack(true) | Uygulamayı arka plana taşır |
| Geri tuşu | onBackPressed hiçbir işlem yapmıyor | Launcher'da tasarlanmış; diğer ekran akışları iyileştirilmeli |

## Kritik eksik/hatalar

1. **Google Play hedef SDK:** Standart Android uygulaması için 31 Ağustos 2026 itibarıyla `targetSdk >= 36` gerekir. Android 15 işletim sistemli araç cihazında çalışması ile hedef SDK aynı şey değildir. Android Automotive OS istisnası normal Android araç teyplerine otomatik uygulanmaz. `compileSdk` ve Android 16 davranışları da güncellenmeli.
2. **İmzalama:** `app/build.gradle.kts` sürüm release'inde debug signing'i kullanıyor. FREE'de yalnız debug, gerçek AAB PRIVATE tarafta gerçek upload key ile üretilmeli.
3. **Gradle Wrapper:** `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` yok. FREE Actions Gradle kurabilse de geliştirici makinesinde bağımsız wrapper bulunmalı.
4. **Kapsamlı işlev eksikleri:** Radar, OBD, araç sensörleri, widget, gerçek AI, sesli dönüş navigasyonu, offline harita indirme mevcut değil.
5. **Yanıltıcı yayın:** “Dini Müzikler” etiketi altında SomaFM Groove Salad örnek stream'i var. Gerçek ad ve kaynak lisansı doğrulanmadan yayınlanmamalı.
6. **Harita/rota sunucu ölçeklenmesi:** Nominatim public API global olarak en çok 1 sorgu/s ve açık istemci uygulamalarında sağlayıcı değiştirebilme/önbellekleme gerekir. OSRM public adresi demo sunucudur; yüksek kullanıcı ölçeği için güvenilir üretim servisi değildir. OSM karo sunucusunun kullanım politikası ve zorunlu atıfları dikkate alınmalı.
7. **Open-Meteo lisansı:** Free endpoint non-commercial kullanıma yönelik; CC BY 4.0 atıf gerektirir. Uygulamanın kullanım modeli ve lisans koşulları netleşmeli.
8. **Konum güvenliği:** Kullanıcı yalnız yaklaşık konum verirse, GPS kapalıysa veya koordinatlar eskiyse arayüz sessizce yanılabilir. Coarse permission, son güncellenme süresi, hata durumu ve durunca hız sıfırlaması eklenmeli.
9. **Yaşam döngüsü:** Harita `onResume/onPause` yalnız composable mount/dispose seviyesinde çağrılıyor; Activity lifecycle ile eşlenmeli. Radio prepare durumu ve dinleme sırasında kesinti yönetimi eksik.
10. **Ekran düzeni:** Bütün kontrol alanları tek ve sabit yerleşimde; farklı en/boy oranları, küçük ekranlar, ekran yoğunlukları ve dokunma güvenliği test edilmeli.
11. **Sürüm/paket:** ZIP'de `com.rasaser.launcher`; mevcut önceki planlarda `com.rasaser.carlauncher` kullanılmış olabilir. Kalıcı paket adı uygulama oluşturulurken kesinleştirilmeli; yayınlandıktan sonra değiştirilmemeli.
12. **Lisans ekranı ve yayın belgeleri:** Uygulama içi lisanslar/gizlilik/üçüncü taraf atıf ekranı yok. FREE şablonuyla entegre edilmeli.
13. **Hata görünürlüğü:** Çok sayıda ağ/GPS/BT hatası sessizce `catch` ile yutuluyor; hata/yok hizmet durumları açıkça gösterilmeli. Cihaz üzerinde test şart.
14. **Tek dosyalı mimari:** Harita, ses, network, ekran, tema ve donanım aynı `MainActivity.kt` içinde; motor arayüzleri ve ayrı Kotlin sınıflarına bölünmeli.
15. **Gerçek testler:** ZIP'de Android unit test, instrumented test, ekran görüntüsü veya derleme kanıtı yok.

## FREE üretim planı

**Aşama 1 — Bağımsız kaynak entegrasyonu:** `apps/carlauncher/` altında ZIP kaynakları birebir incelenerek güvenli, ayrı Android projesine taşınmalı. İlk adımda kaynakların gerçekten derlendiği kanıtlanmalı; sabit özellikler korunmalı.

**Aşama 2 — Android ve kaliteli UX:** target/compile API 36, Android 15 cihaz uyumluluğu (minSdk 26 korunabilir), Gradle/Compose güncelleme, responsive landscape, izin/konum, medya, offline/online hata davranışı, kalıcı dil/tema.

**Aşama 3 — Gerçek işlevler:** Radar/OBD/Widget gibi yer tutucular **ya gerçek entegrasyon ve testle** çalışır hâle getirilmeli **ya da yayın arayüzünden kaldırılmalı**. “AI” etiketi sesle adres aramadan fazlası yoksa yeniden adlandırılmalı. Navigasyonda cihaz güvenliğine öncelik verilmeli.

**Aşama 4 — Ücretsiz ve lisans uyumlu servisler:** Harita, rota, hava durumu ve radyo sağlayıcıları kullanım koşulları/kotaları altında değerlendirilmeli; public demo sunucuları sınırsız üretim hizmeti sayılmamalı.

**Aşama 5 — GitHub Actions:** PUBLIC depoda secret olmadan birim testleri ve debug APK. PUBLIC Actions içinde PRIVATE motor veya özel signing anahtarı kullanılmaz.

**Aşama 6 — PRIVATE yayın:** Onaylı public commit + sürümlü özel motor güvenli ortamda birleştirilir; release testi, AAB upload imzası, SHA-256, sürüm ve bağımsız şifreli yedek; Play Console testleri ve yayın.

## Referanslar
- Google Play 2026 hedef API: https://developer.android.com/google/play/requirements/target-sdk
- Nominatim Kullanım Politikası: https://operations.osmfoundation.org/policies/nominatim/
- OSM Tile Politikası: https://operations.osmfoundation.org/policies/tiles/
- Open-Meteo Terms: https://github.com/open-meteo/open-meteo
- OSRM demo sunucusu: https://github.com/Project-OSRM/osrm-backend

Bu belge gerçek APK çalıştırma veya cihaz testinin yerini tutmaz.
