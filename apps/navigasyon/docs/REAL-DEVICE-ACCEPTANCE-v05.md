# RaSaSeR Navigasyon v0.5 - gerçek cihaz kabul testleri

Bu belge saha testi yapılmış olduğu anlamına gelmez. Vektör harita ve Valhalla yol grafiği GitHub Releases'de açık lisanslı ve sürümlü dosyalardır. Mobil test mutlaka yapılmalıdır.

## Fonksiyon doğrulaması

1. Android 15 telefon / Android 15 araç multimedya: yatay ve dikey ana ekran; kum-beyaz düğmeler; TR, EN, DE, AR, ES, JA geçişleri.
2. İlk indirmede 606 MB ZIP'in, 670 MB MBTiles'ın SHA-256 ve boyut doğrulaması. İndirme yarıda kalırsa ağ geri geldiğinde sürdürme; düşük depolama; bozuk ZIP reddi.
3. Ayarlarda Valhalla 1,15 GB yol grafiği indir ve SHA-256 doğrula. Harita çalışırken rota grafiği yoksa açıkça uyarı göster.
4. Uçak moduna alınan cihazda GPS ile çevrimdışı harita ve İstanbul/Ankara/Uşak şehir içi ve şehirler arası gerçek güzergâh; rota şekli yolları takip etmeli.
5. Sürüşte hedef belirlenmese bile yakınlaşmış 3D kamera, hız ve işletme/durak isimleri (yalnız veride mevcutsa) çalışmalı.
6. Geçişte yaklaşan dönüş talimatı, dönüşe yakın ikinci ses, sesli tekrar spam olmamalı; konum kaybında hayalî dönüş bildirmemeli.
7. Rotadan sapıldığında üç güvenilir GPS sabitlemesi + aralık ile offline yeniden rota. Üst üste konum sıçraması yanlış rota üretmemeli.
8. Yol tabelasında açık hız limiti bulunan belirli kesitlerde Valhalla matched edge.speed_limit ile karşılaştırma. Veride yoksa bilinmeyen gösterilmeli. Hız aşım alarmı GPS sapmasında tekrar etmemeli.
9. Ekran yönü değiştirme, kapatıp açma ve araç boot bildirimi üzerinden hedefe geri dönme. Android sistemin boot sonrası UI başlatma sınırlamaları geçerlidir.
10. Türkçe, İngilizce, Almanca, Arapça, İspanyolca ve Japonca gerçek **çevrimdışı ses** kurulu ise TTS; kurulu değilse açık hata. Arapça ve Japonca yer adları harita/fontlarıyla gözle doğrulanmalı.
11. İşletme, market, durak, geçit ve diğer POI kapsamı OSM/Shortbread ile sınırlıdır; her gerçek işletmeyi gösterme iddiası test kapsamı dışında.
12. Canlı trafik, mobil radar, kapalı yol ve gerçek zamanlı kazalar için doğrulanmış ücretsiz kaynak mevcut değilse **aktif veri varmış gibi** ikon/ses üretme.

## Güvenlik ve yayın koşulları

- Ana repo korunur, bu uygulamanın kaynakları yalnız public RASASER-FREE/apps/navigasyon klasöründe.
- Saha testleri Android 15 ve Android 8+ (API 26) cihazda yapılmalı.
- En az 3 GB depolama ve Wi-Fi ile iki ayrı büyük veri paketi; alternatif USB aktarım yeteneği ileride rota paketi için de eklenecek.
- Google Play / üretim için imzalı optimize release, ayrı güvenli imza deposu ve harita ODbL/SIL OFL atıf ekranı zorunlu.
- Ücretsiz açık harita API'si = hizmet kullanım başına ödeme yok; internet trafiği ve dosya dağıtımı bütçesi ayrı değerlendirilmelidir.
