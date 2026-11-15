# Türkiye tam harita paketi / kendi veri kaynaklarımız

Öncelikli harita: Türkiye ülke genelindeki OSM verilerinden BBBike tarafından hazırlanmış Shortbread vektör MBTiles.
Kaynak: https://data.bbbike.org/osm/region/europe/turkey/
Açık veri lisansı: https://www.openstreetmap.org/copyright (ODbL 1.0)

`prepare_turkey_shortbread.py` ülke ZIP dosyasını indirir, içindeki MBTiles'ı CRC ile açar,
SQLite bütünlüğünü, vektör formatını ve 10 kent merkezi için karo varlığını kontrol eder.
`turkey.manifest.json` dosyasında SHA256, boyut, veri kapsamı ve kaynak bilgileri yer alır.

İlk oluşturulacak GitHub Release: `rasaser-turkiye-maps-v0.1`. Bu kalıcı **kullanıcı tarafından indirilebilir harita veri paketi** olacak;
Android navigasyon uygulaması veya cihaz içi offline yol tarifi değildir.

Açık kaynak projelerin lisanslarına uyum, sürümlü güncelleme ve Android cihaz içi SQLite/TMS karoları
Shortbread kaynak stiline bağlamak sonraki geliştirme adımlarıdır.

**Faturalandırma koruması:** GitHub Actions artifact depolaması kullanılmaz. Binaries kaynak Git geçmişine
işlenmez. Yalnız GitHub Release dosyası olarak tutulur; böylece 500 MB artifact limitini aşmayız.
GitHub Releases her dosya için 2 GiB sınırı tanır ve toplam sürüm boyutu ve bant genişliğine sınır
koymadığını bildirir: https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases
