# Türkiye öncelikli uygulama yol haritası

## Araştırılmış adaylar (15 Kasım 2026)
- **Tilemaker**: OSM özütünden hafif, ticari sözleşmesiz MBTiles. Zayıf yönü: artımlı diferansiyel veri güncellemesi için uygun değil; ülke bazında yeniden üretim.
- **Planetiler**: yüksek hacimli üretimde alternatif; MBTiles/PMTiles üretebilir. İkinci ölçümde veri hacmi, RAM ve süre karşılaştırılacak.
- **MapLibre Native Android**: 3D binalar, vektör tabanlı zoom, kendi renk paleti. Harita API'larına bağımlı değil.
- **Valhalla**: C++ hiyerarşik rota grafikleri, offline hedefe uygun; Android NDK derleme ve performansı ayrıca test edilmeli.
- **GraphHopper**: offline Android desteği artık resmen sürdürülmüyor; bu nedenle ilk sırada değil.
- **Organic Maps**: olağanüstü offline referans; hazır veri dosyaları kendi ayrı lisansına/markalama kurallarına bağlı. Doğrudan kopyalama seçilmedi.

## Türkiye gerçek doğrulama sırası
1. Basit küçük bölge/düşük zoom smoke test, sonra tam Türkiye üretimi.
2. İstanbul, Ankara, İzmir, Uşak/Banaz, Van sınır/kırsal yollar dahil kontrollü saha verisiyle karşılaştırma.
3. İller arası köprü/tünel geçişleri ve kopuk otoyollar testleri.
4. 81 il sınırı için lisanslı sınır verisi, en az 10-30 km dış tampon ve sınırlar arası tek rota grafiği tasarımı.
5. Yol adı, yapı yüksekliği boşlukları, POI kaynak kapsamı raporu.
6. Android 15 telefon ve araç ünitesi testleri; düşük RAM, çevrimdışı uçuş modu ve GPS.
7. Sadece testli ve imzalı Release çıktısı; ana özel depoda imzalama.

## Kesinlikle doğrulanmadan 'hazır' sayılmayacaklar
- Haritaların tamamının gerçekten oluşturulması
- Dinamik trafik ve radar verisi
- 81 ilin eksiksiz detay paketi
- Türkçe offline TTS kullanılabilirliği (cihaz motoruna bağlı)
- Android sürüş navigasyonu, yasal trafik doğruluğu, cihazlarda stabilite
