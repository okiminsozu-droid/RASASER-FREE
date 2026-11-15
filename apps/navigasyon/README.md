# RaSaSeR Navigasyon — Türkiye / v0.1 veri fabrikası

**Durum (2026-11-15):** Bağımsız Türkiye harita veri üretim altyapısı. Burada **henüz çalışan navigasyon APK'sı, tamamlanmış Türkiye haritası veya Android'e entegre offline rota motoru yoktur.** Yazılım ilerleyişi testlere göre belgelenecektir.

## Bağımsızlık koşulları

- Android telefon + Android araç multimedya ekranı; Android 15 hedef, minSdk 26 plan.
- Zorunlu Google Maps, Mapbox, MapTiler, ücretli API, abonelik veya bulut hesabı **yok**.
- Dışarıdan **harita veri lisansı satın alınmayacak**. OSM ODbL lisansına ve Tilemaker/MapLibre/Valhalla üçüncü taraf bildirilerine uyulacak.
- Map rendering: **MapLibre Native Android** (açık kaynak). Üretim: **Tilemaker** (vektör MBTiles). Rota: **Valhalla** adayı (Android NDK offline entegrasyonu/benchmark henüz gerekli).
- Offline haritalar *bir kez indirilen/aktarılan* dosyalardan, cihazın dahili depolamasından okunacak. GPS/GNSS internet gerektirmeden çalışabilir; cihazın konum sağlayıcısına bağlıdır.
- Ana harita, rota, veri üretimi, dizinler, uygulama kimliği ve derleme diğer tüm RaSaSeR uygulamalarından bağımsız.
- Özel RASASER deposuna, mevcut uygulamalara, mevcut CI iş akışlarına ve hiçbir imzalama anahtarına dokunulmaz.

## İlk çalıştırılabilir bölüm: Türkiye harita fabrikası

Linux bilgisayarda Docker + curl + unzip + Python 3 + md5sum + sha256sum gereklidir. Yaklaşık 650 MB OSM kaynak verisi ile kıyı/landcover varlıklarını indirir; üretim için **yüksek boş disk, SSD ve yeterli RAM** gerekir. Kesin ihtiyacın ölçülmesi gerekir.

```bash
cd apps/navigasyon
bash data/build-turkey.sh
```

Bu işlem **kullanıcının başlattığı cihazda** büyük indirmeler ve yoğun hesaplama yapar. GitHub Actions üzerinde otomatik olarak büyük Türkiye verisi üretimi/indirmesi **yapılmaz**. Varsayılan açık Docker resmi `ghcr.io/systemed/tilemaker:master` kullanılır; yeniden üretilebilir sürüm için onaylı image digest'i `TILEMAKER_IMAGE=...` ile sabitleyin.

Beklenen yerel çıktılar (Git'e gönderilmez):
- `data/work/output/turkey.mbtiles` — kendi üretilmiş vektör harita paketi
- `data/work/output/turkey.manifest.json` — kaynak checksum, dosya checksum ve meta
- `data/work/archive/` — önceki **doğrulanmış** paketler, otomatik silinmez

`data/verify_mbtiles.py` formatı, SQLite okunabilirliğini, temel meta veriyi ve en az bir örnek vektör karoyu kontrol eder. **Tüm şehirlerin yol kalitesi, dönüş yasakları, bina yükseklikleri ve güzergâh güvenliğini doğruladığı iddia edilmez.**

## Harita görüntü stili

`styles/rasaser-day.json` ve `styles/rasaser-night.json` dosyaları Tilemaker'ın OpenMapTiles uyumlu katmanlarını kullanan başlangıç MapLibre stilleridir. Tamamen **yerel** `127.0.0.1:8765` karolarını bekler; üretim uygulamasında MBTiles için cihaz üzerindeki yerel karo sağlayıcı (TMS/XYZ dönüşümü ve gzip türü dahil) eklenmelidir. Henüz kurulmuş bir servis veya mobil ekran yoktur. Fontları çevrimdışı lisanslı biçimde eklemeden ad/yol etiketi çizimi bilinçli olarak kapalıdır.

## Geliştirme kapıları

1. Türkiye harita paketi gerçek cihazda ölçülüp doğrulanacak; şehirler, kıyılar, köprüler, tüneller, binalar kontrol edilecek.
2. Aynı OSM sürümünden bağımsız **offline rota grafiği** üretilecek; dönüş yasakları ve rota sapması testleri.
3. Android'de offline MBTiles okuma + MapLibre ekranı + yakınlaştırma/3D bina + hafıza testleri.
4. GPS kaydı, konum izni, TTS Türkçe manevra, izleme ve yönlendirme kalite testleri.
5. 81 il paketleme sınırları (iller arası yollar kesilmemeli), sürümlü güncellemeler, indirilebilir paketler.
6. Release AAB/APK imzalama sadece PRIVATE ortamda; kişisel veriler dışarı aktarılmayacak.

Kamera, radar, sabit hız sınırı, gerçek zamanlı trafik gibi bilgilerin kapsama/doğruluğu **ayrıca değerlendirilmelidir**. Ücretli veya izinsiz veri çekimi yapılmayacaktır.

Referanslar: https://download.geofabrik.de/europe/turkey.html · https://github.com/systemed/tilemaker · https://github.com/maplibre/maplibre-native · https://github.com/valhalla/valhalla · https://www.openstreetmap.org/copyright


## Android hibrit bağlantı prototipi v0.2 (15 Kasım 2026)

Bağımsız `com.rasaser.free.navigasyon` Android 15 hedefli geliştirici uygulaması eklendi: internetin otomatik algılanması, `VALIDATED` internet / ölçülü bağlantı ayrımı, çevrimdışı navigasyon önceliği, interneti kapat/aç kullanıcı tercihi, cihazda Türkiye paketinin SHA-256 kontrolü. **Bu henüz harita gösteren veya rota çizen bir APK değildir.** Veri ve rota indirme sistemleri, MapLibre/Valhalla mobil entegrasyonu yapılmadı. Bkz. `docs/HYBRID-OFFLINE-ONLINE.md`. Özel imzalama yapılmaz.

## Sürüş ekranı, altı dil ve ilk kurulum — v0.3 geliştirici derlemesi

- Kum rengi / beyaz düğmeler, sağ üst dil listesi: TR/EN/DE/AR/ES/JA. Tüm birinci taraf arayüz metinleri ve uyarıları için altı dilde çeviri.
- İlk ekranda **Haritayı yükle, İzinleri ver, Başla**. Konum, mikrofon, bildirim izinleri bir tuşla izin akışına gönderilir; Android ayrı onay ister. DND erişimi ayrıca sistem ekranındadır.
- İlk harita: GitHub Releases Türkiye Shortbread 606 MB ZIP; DownloadManager ile Wi-Fi öncelikli, mobil veride ayrı onay, USB dosyası için yerel içe aktarma. ZIP ve MBTiles SHA-256 sabit değerlerle doğrulanır.
- **Başla** sonrası MapLibre Native Android 13.7.1 OpenGL ES, çevrimdışı MBTiles ve 127.0.0.1 yerel sunucu; GPS ile 3D sürüş kamerası ve mevcut hız.
- Haritanın altında ayarlar, mikrofon ve uyarı ikonları. Kullanıcı isteğe bağlı çevrimiçi destek, Wi-Fi, ses ve araç açılış tercihlerini düzenleyebilir.
- Sesli komut sadece cihazda on-device recognizer varsa, çok dilli TTS ise ilgili çevrimdışı ses verisi destekleniyorsa kullanılabilir.
- Android 10+ arka planda zorla tam ekran açılışı kısıtlar. Araç başlatma hatırlatıcısı isteğe bağlı bildirim şeklindedir.
- POI ve işletme kapsamı OpenStreetMap verisiyle sınırlı; tüm şirketleri eksiksiz göstermeyi garanti etmez.
- **Henüz tamamlanmayanlar:** Android offline rota/Valhalla, sesli dönüş yönlendirmeleri, doğrulanmış hız sınırları, canlı trafik/radar, ülke çapı sürüş testleri ve harita yazıları için offline font glyph paketi.

Bu geliştirme aşamasıdır; gerçek yol testleri tamamlanmadan yayınlanmamalıdır.

## Çevrimdışı Türkiye sürüş ve rota geliştirmesi v0.4 (2026-11-15)

- Açık lisanslı Noto Sans fontu GitHub Android build sırasında tüm 256 PBF glyph aralığı olarak paketlenir; MapLibre'nin sokak, bölge, POI ve toplu taşıma durak adları internet olmadan okunabilir. Verinin hiç içermediği işletme adları oluşturulmaz.
- Harita stilleri seçilen TR/EN/DE/AR/ES/JA diline göre varsa OSM `name_<dil>` ad alanını tercih eder, yoksa özgün yer adını kullanır; dünya çapında tüm özel isimlerin çevrildiği iddia edilmez.
- Cihazda çalışan Valhalla Mobile (MIT, 0.6.1) rota motoru eklendi, haritaya dokunma/uzun basarak hedef seçme ve yalnız gerçek grafikten hesaplanan yol varsa mavi güzergâh çizimi. Güzergâh doğrudan çizgiyle uydurulmaz.
- Valhalla Türkiye yolu ham Geofabrik OSM verisinden oluşturulur: `valhalla_tiles.tar` 1.148.661.760 bayt. Başlangıç Release manifesti graph SHA256 `bdfd9a67d8fdd0d5856013e889c6e48cd8a14560934cc5d2b6f3c16a8f6a554f`. Route installer bu özeti sabit olarak doğrular; en az 3 GB disk alanı ve Wi‑Fi gerektirir.
- Rota grafiği, ilk 606 MB harita paketinden **ayrıdır** ve ayarlardan kullanıcı tarafından indirilir. Grafik ayrıca `valhalla.json` gerektirir; sürümde bu dosya olmadan uygulama güvenli biçimde 'rota verisi yok' mesajı gösterir.
- İlk manevranın sesli okunması desteklenir ancak GPS üzerinden sürekli doğru dönüş/yeniden rota yönlendirmesi ve dinamik hız limitleri **tamamlanmış değildir**.
- Android cihaz/araç üzerinde GPS saha testi **yapılmadı**; app bir geliştirme sürümüdür. Play Console için release imzası yoktur. Test sonuçlarını `05-navigasyon-android-ci.yml` üzerinde kontrol edin.
- Dışarıdan ücretli harita/routing/voice API'si yoktur. Cloudflare R2 zorunlu değildir. İlerideki veri paketleri sürümlü ve SHA256 doğrulanmış olmalıdır.

Release SHA256'in GitHub tarafından bildirilen gerçek varlık özeti esas alınır (`bdfd9a67d8fdd0d5856013e889c6e48cd8a14560934cc5d2b6f3c16a8f6a554f`). Grafiğin her yeni üretiminde TAR SHA256 değişebilir, yayımlanmış graph varlığının digest'iyle eşleşmeyen manifest kullanılmaz.
