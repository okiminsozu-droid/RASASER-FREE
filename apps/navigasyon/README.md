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
