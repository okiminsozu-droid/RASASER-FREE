# Türkiye Shortbread haritası için RaSaSeR stilleri

Doğrulanan 420.189 karoluk Türkiye paketi BBBike Shortbread şemasındadır. İlk Tilemaker taslağındaki OpenMapTiles `transportation/building` katmanları bu pakete **uygulanamaz**. Buradaki yeni iki stil `streets, buildings, water_polygons, ocean, water_lines, land, sites, place_labels, street_labels` katmanlarını kullanır.

- `rasaser-shortbread-day.json` gündüz
- `rasaser-shortbread-night.json` gece
- Stil kaynak URI'si `127.0.0.1:8765` cihaz üzerinde offline MBTiles okuyan **henüz yapılmamış** servis tarafından sağlanmalıdır.
- JSON stilindeki `glyphs` yolu için Open Font License vb. uyumlu, çevrimdışı glyph PBF paketleri üretilmeli ve cihaza konmalıdır. Fontlar henüz dahil değildir; kaynak gösterimi yayında korunmalıdır.
- MBTiles standardında y koordinatı **TMS**, stil erişiminde **XYZ**; yerel sunucu bunları doğru dönüştürmelidir.
- Orijinal Shortbread 1.0 `buildings` katmanında **gerçek bina yüksekliği yoktur**. Stil şimdilik 8m varsayılan perspektif yükseltme kullanır; gerçek yükseklik gibi tanıtılmamalı. Gerçek 3D için yükseklikli farklı üretim gerekir.
- Verilerin ülke geneli olduğunu söylemek, her sokak ve hız sınırının eksiksiz/gerçek olduğunu garanti etmez.

Şema: https://shortbread-tiles.org/schema/1.0/
Örnek: https://github.com/shortbread-tiles/shortbread-demo-maplibre
Veri: https://data.bbbike.org/osm/region/europe/turkey/
