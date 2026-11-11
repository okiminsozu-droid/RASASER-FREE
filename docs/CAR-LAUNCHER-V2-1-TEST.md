# CAR LAUNCHER V2.1 — İstenen düzeltmeler / korunmuş sürüm

**Kapsam:** Kullanıcının verdiği siyah-altın CAR LAUNCHER arayüzü ve çalışan modüller aynen korunarak yalnız bildirilen eksikler düzeltildi. Tüm değişiklikler `feature/carlauncher-v2-1-stability-icon` dalındadır. `main` ve `archive/carlauncher-original-interface-20261110` değiştirilmedi. Private RaSaSeR deposuna dokunulmadı.

## Uygulananlar
1. **Uygulama logosu:** Kullanıcının onayladığı siyah-altın CAR LAUNCHER görselinin küçük ekran için optimize edilmiş JPG kopyası `apps/carlauncher/v2/assets/car_launcher_icon.jpg` altında saklandı, SHA-256 ile doğrulanarak Android uygulama ikonuna bağlandı. Asıl yüksek çözünürlüklü görsel ayrı korunuyor; Play Store grafik seti yayın öncesi 512px olarak hazırlanmalı.
2. **Online Radyo kartı:** Camii simgesi çıkarıldı, altın çizgili radyo simgesi koyuldu. İstasyon önceki/sonraki ve oynat-duraklat düğmeleri kart içinde görünür ayrı dokunma alanları olarak konumlandırıldı. Ülke seçimi artık ayrı bir sayfa değil, ana ekran üzerinde açılan aramalı ülke penceresi; Compose DropdownMenu+LazyColumn intrinsic ölçüm çökmesi önlendi.
3. **Sürüş haritası:** Sol Harita & Rota düğmesi ve haritadaki araç düğmesi, mevcut ana ekranın osmdroid harita penceresinde takip modunu açar, konumu merkezler ve yaklaştırır. Büyük araç/yön oku ekranın içindedir. GPS yönüne göre harita baş-yukarı dönmek için eksi-bearing dönüşünü kullanır. Google Maps kısayolu ve diğer mevcut harita işlevleri silinmedi.
4. **Alt butonlar:** Kullanıcının isteği doğrultusunda yalnızca **Ekran Yansıtması** kaldırıldı. Uygulamalar, Widget ve Kapat düğmelerinin eski Unicode simgeleri yerine çizilen altın vektör simgeler eklendi. Play Store, TV, dosyalar, kamera, kayıt, Bluetooth, +ekleme, temalar ve diğer düğmeler korundu.
5. **Test ayrılığı:** V2.1 uygulaması `com.rasaser.launcher.v21test` debug paket kimliğiyle derlenir. Mevcut V1/V2 cihaz kurulumu üstüne yazılmaz, üretim imzalama anahtarları kullanılmaz.

## Doğrulama
- [x] Orijinal ZIP'in 10 dosyası değişmeden korunuyor (source-packed SHA-256).
- [x] Değişiklikler yalnız yeni V2.1 feature dalında ve FREE deposunda.
- [ ] GitHub Actions V2.1 **son ikonlu** debug derlemesi başarılı.
- [ ] Ülke listesine dokunulduğunda uygulama ana ekrana atmıyor (Android 15 gerçek cihaz testi).
- [ ] Türkiye/Almanya/Japonya ülkeleri listeleniyor ve önceki/sonraki istasyonlar çalışıyor (ağ + cihaz testi).
- [ ] Harita sürüş modu uygulamanın kendi penceresinde çalışıyor (GPS + cihaz testi).
- [ ] Yeni uygulama simgesi cihaz ana ekranında görünüyor.
- [ ] Alt vektör simgeleri cihaz çözünürlüğünde okunabilir, ekran yansıtma kaldırıldı.
- [ ] Çalışan diğer bölümlerin gerileme testi tamamlandı.

**Not:** Başarılı bir Gradle derlemesi, cihaz üzerinde radyo yayınları, GPS ve dinamik menü davranışının tamamını kanıtlamaz. Bu bir DEBUG test APK'sıdır; Play hedef API ve AAB imzalama işlemleri ayrıca yapılacak.
