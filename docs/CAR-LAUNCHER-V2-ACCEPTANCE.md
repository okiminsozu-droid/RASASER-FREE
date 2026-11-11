# CAR LAUNCHER V2 — Koruma kaydı ve cihaz kabul testleri

**Durum:** Android 15 (API 35) araç multimedya ünitesinde ilk cihaz testi yapılmadan üretime/yayına alınmayacak. İlk V2 debug derlemesi GitHub Actions run #42897347350 üzerinde başarılı. Son eklemelerin ayrı testleri sürüyor.

## Değişmez koruma ve yayın sınırı

- Kullanıcının verdiği orijinal ZIP kaynakları `apps/carlauncher/source-packed/` içinde SHA-256 doğrulamalı, **değişmeden** korunuyor.
- Özgün ekranın anlık görüntüsü ayrı `archive/carlauncher-original-interface-20261110` dalına alındı.
- Son başarılı **orijinal APK** main dalında ve Actions #42889219238 geçmişinde korunuyor.
- V2 yalnızca `feature/carlauncher-dashboard-v2` dalında, `apps/carlauncher/v2/` altındaki ek kod ve derleme sırasında uygulanan ayrı yama ile oluşturuluyor.
- RaSaSeR **PRIVATE** ana deposu, mevcut motorlar, diğer uygulamalar ve imzalama anahtarları değiştirilmedi.
- Debug testinin paket kimliği `com.rasaser.launcher.v2test`; orijinal uygulamanın üzerine yazmayacak. PRIVATE yayın aşamasında orijinal `com.rasaser.launcher` korunacak.
- Nihai yayın sürümünde debug imzası kullanılamaz; güvenli upload key, optimize AAB, Play target API 36 kontrolü gerekir.

## Eski arayüzden korunanlar

- Siyah-altın geniş ekran şablonu, ana üst şerit, saat/tarih/bağlantı durumları, dil bayrakları
- Sol menü, orta harita kutusu, hız göstergesi, radar kutusu, radyo kartı, hava durumu kartı, sağ kısayol kartı, alt araç çubuğu
- Özgün radyo kartı resmi (🕌) ve mevcut tema paleti
- GPS, rota arama, sesle adres arama, harita zoom, saat, hava durumu, telefon, kamera, kayıt, dosyalar, yansıtma ve Bluetooth kısayolları

## Eklenen / değiştirilen işlevler

1. **Açılış:** Android Launcher / HOME rolü orijinal manifestte var. Kullanıcı ayarlardan **varsayılan ana ekran** seçerse cihazın ana ekranı olarak açılır. Android 15'in arka plan başlatma kısıtlamaları nedeniyle yetkisiz boot receiver ile zorla açılmaz.
2. **Diller:** TR/EN/DE/ES/JA, dil butonuyla anlık ekran çevirisi ve seçim saklama.
3. **Sürüş:** GPS konumunu takip eden harita, hareket yönüne göre dönme, ekranda belirgin ▲ araç oku, mahalle/sokak adı için Android Geocoder, açık haritada mevcut zoom/rota işlevleri.
4. **Google Maps:** Var olan uygulama kısayoluna ek olarak harita üstünde G düğmesiyle Google Maps konumunu açma; **Google Maps verilerini/harita SDK'sını bu ekrana gömmek için API anahtarı ve sağlayıcı şartları ayrıca gerekir.**
5. **Radar:** OSM `highway=speed_camera` topluluk noktalarının düşük sıklıklı Overpass sorgusu; tespit edilirse 500 metre mesafede tek sesli uyarı. Google Maps'in özel radar konumları kullanılmaz, verinin eksiksiz/doğru olduğu garanti edilmez. Radara yaklaşılmıyorsa kullanıcı yanıltılmaz.
6. **Radyo:** Eski radyo kartı içinde ülkelerin bayrak listesi, ülkedeki çevrimiçi istasyonlar, ileri/geri ve oynat/duraklat. Veri kaynağı bağımsız Radio Browser dizini; internet ve uygun yayın gerekir.
7. **TV:** Orta ekranda **yerinde** küçük kanal listesi ve oynatıcı; başka uygulamaya yönlendirme yok. Alt TV simgesine 3 saniye basılınca kapanır; ✕ da kapatır. Kaynak IPTV-org kamuya açık bağlantı dizini; kanalların yayın hakkı, reklam içermeme durumu ve sürekliliği **otomatik garanti edilemez**. GPS ile durduğu doğrulanmamış araçta video oynatma engellenir.
8. **Kısayollar:** Sağ kart dikey kayar, üstteki + ve alt bardaki + aynı uygulama seçicisini açar, eklenen uygulamalar kendi ikonlarıyla ve kalıcı olarak gösterilir. Basılı tutunca kısayolu kaldır veya Android'in onaylı kaldırma ekranı açılır; uygulama kendiliğinden silinmez.
9. **Widget:** Alt bölümden açılan gerçek Android AppWidgetHost; cihazdaki üçüncü taraf widget sağlayıcılarını listeler, kullanıcı sistem onayıyla ekler, saklar ve kaldırır. Destek, ilgili widget sağlayıcısına ve araç multimedya işletim sistemine bağlıdır.
10. **Alt araç çubuğu:** Navigasyon → Play Store, Radyo → Uygulamalar, eski video → Canlı TV, araç bilgisi ve OBD kaldırıldı, + uygulama eklendi. Diğer tuşlar korunuyor.

## Cihaz test listesi

- [ ] V2 kuruldu, orijinal test kurulu kaldı
- [ ] Yatay arayüz ve siyah-altın kart ölçüleri özgün tasarımla aynı
- [ ] Beş bayrağın tamamında görünen metinler değişiyor ve seçim yeniden açınca korunuyor
- [ ] Android HOME varsayılan uygulama olarak seçildi ve yeniden başlatıldığında otomatik açıldı
- [ ] GPS izni verildi, büyük ok merkezde, hız/konum/mahalle güncelleniyor
- [ ] Harita aç/kapa, rota, arama, sesli adres ve Google Maps G kısayolu çalışıyor
- [ ] Radar kaynağında doğrulanmış kayıt varsa 500 m uyarısı geliyor
- [ ] Radyo ülkeleri, istasyon listeleri, önceki/sonraki, internet kesildiğinde hata davranışı
- [ ] TV listesi ve player aynı ekranda, 3 saniye basılı tutarak kapatma, hareket hâlinde görüntü engeli
- [ ] + ile uygulama, gerçek ikon, kısayolu kaldırma, Android sisteminden uygulama kaldırma
- [ ] Widget listesi, izin onayı, ekleme/saklama/kaldırma
- [ ] Sol menü, saat, hava durumu, tema, telefon, kamera, kayıt, dosyalar, yansıtma, Bluetooth işlevleri eskisi gibi çalışıyor
- [ ] 1024×600 ve hedef araç ekranı çözünürlüğünde taşma/ekran dışına çıkma yok
- [ ] Play Console yayımlama gereksinimleri ve üçüncü taraf lisans atıfları kontrol edildi

## Son başarılı V2 test APK kaydı

- **GitHub Actions:** #42899260018 — **SUCCESS**
- **Artifact ID:** `13802840782`
- **APK:** `app-debug.apk`, 11.985.343 bayt
- **SHA-256:** `468ec700cc553ae23e45fc62b4252946cb586e284a38171a60dba5cf3d3c33b6`
- **Uygulama paketi:** `com.rasaser.launcher.v2test` (orijinal test uygulamasının üzerine yazmaz)
- **Sürüm:** `1.1.0-dashboard-test-test` (debug), `versionCode=2`
- **Kaynak koruması:** Orijinal sıkıştırılmış 10 dosya değişmeden korundu; arayüz V2 yamaları ayrı `v2/` altında.
- **Cihaz testi:** Henüz yapılmadı; bu APK Google Play'e gönderilecek yayın paketi değildir.
- **Doğrulanan:** CI kaynak bütünlüğü, Android/Kotlin Gradle debug derleme, paket ZIP bütünlüğü ve `AndroidManifest.xml` ile `classes.dex` mevcudiyeti.

**Not:** Bu listede doğrulanmamış maddeler tamamlanmış gibi işaretlenmemiştir.
