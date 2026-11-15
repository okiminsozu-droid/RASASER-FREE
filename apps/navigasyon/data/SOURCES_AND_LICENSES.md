# Veri kaynakları, lisans ve yayın kuralları — Türkiye

**OSM harita verisi:** © OpenStreetMap contributors, [ODbL 1.0](https://www.openstreetmap.org/copyright). Geofabrik (https://download.geofabrik.de/europe/turkey.html) Türkiye'nin OSM veri özütünü sağlar. Geofabrik ek kullanım koşulları ve kaynak/katkı atıfları gözden geçirilir.

**Üretim:** [tilemaker](https://github.com/systemed/tilemaker) FTWPL; kendi hazır `process.lua` dosyası OpenMapTiles işleme şemasını temel alır ve CC-BY atıfları barındırır. Upstream lisans açıklamaları üretim kaydı ve uygulama Lisanslar sayfasında tutulmalıdır.

**Kıyı çizgisi:** OpenStreetMap tabanlı https://osmdata.openstreetmap.de/download/water-polygons-split-4326.zip — indirme sayfasının kaynak/lisans koşullarını kontrol edin.

**Arazi katmanları:** Natural Earth https://www.naturalearthdata.com/ üzerinden Nacis CDN arşivleri (ne_10m_urban_areas, ne_10m_glaciated_areas, ne_10m_antarctic_ice_shelves_polys). Dağıtım öncesi tüm kaynak metadataları doğrulanmalıdır.

**Görselleştirme:** [MapLibre Native](https://github.com/maplibre/maplibre-native); yerel karo endpoint'i kullanılacak.

**Rota:** [Valhalla](https://github.com/valhalla/valhalla) MIT; henüz Android offline entegre edilmedi.

## Yayın ilkeleri

1. Harita ekranında görünür ve tıklanabilir `© OpenStreetMap contributors` atfı, uygulamanın Hakkında/Lisanslar ekranında tam lisans bağlantısı bulunmalı.
2. ODbL'den türemiş veri tabanlarının paylaşım/erişim yükümlülükleri yerine getirilmeli. Uygulamanın kendi yazılım koduyla veri lisansı birbirine karıştırılmamalı.
3. Hazır üçüncü taraf uygulamanın marka, arayüz veya **özel lisanslı .mwm / tile** dosyalarını kopyalamayın. Organic Maps hazır veri ve white-label izinleri, bu projenin bağımsız veri üretim tercihine uygun olmadığı için kullanılmaz.
4. Kullanım ücretinin olmaması, internet/disk/cihaz ve dağıtımın hiçbir maliyeti olmayacağı anlamına gelmez; **ücretli harita API'sı zorunlu değildir**.
5. OSM'de mevcut olmayan sınır, gerçek hız, trafik ve kameraları doğruymuş gibi gösterme. Veri tarihini kullanıcıya göster.
