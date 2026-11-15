# Türkiye offline rota grafiği - Valhalla

Rota dosyası, yayımlanmış 606 MB Türkiye Shortbread harita görüntüsünden **bağımsızdır**. Shortbread görsel karolarında OSM dönüş yasakları/hız limitleri bulunmaz. Doğru yasal dönüş ve yön tarifi için ham OSM -> Valhalla grafiği gereklidir.

Üretim workflow: .github/workflows/07-navigasyon-turkiye-rota-uret.yml
Çıktı (başarılı olursa): GitHub Releases rasaser-turkiye-routing-v0.1, valhalla_tiles.tar.
İş: ham Geofabrik Türkiye OSM PBF + md5, Valhalla scripted Docker, örnek graph doğrulama, kaynak SHA256 ve graph SHA256.
GitHub Actions artifact depolaması kullanılmaz, gizli imza anahtarı yok. Çıktılar yalnız kaynak repo public Releases.

Android tarafında kullanılacak motor: https://github.com/Rallista/valhalla-mobile (MIT).
Örnek sürüm io.github.rallista:valhalla-mobile:0.6.1 (desteklenen formatları ve Android derleme uyumluluğunu ayrıca test etmek şartıyla).

**Sürüş güvenliği:** Bütün yönlendirmeleri gerçek cihazla doğrulamadan güvenilir navigasyon sayılmaz. Çevrimiçi rota servisi gerekmeyecek. Hız limitleri, radar ve canlı trafik otomatik uydurulmayacak.

Graph oluşturma yüksek RAM/disk/CPU gerektirebilir; GH runner kapasitesi yetersizse işlemi ücretsiz yerel Linux bilgisayarda yürütmek gerekir. Olası başarısızlık durumunda Android navigasyonun mevcut harita paketi korunacaktır.
