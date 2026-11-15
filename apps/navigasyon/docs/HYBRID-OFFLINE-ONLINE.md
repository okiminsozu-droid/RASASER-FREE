# Hibrit çevrimdışı / çevrimiçi navigasyon tasarımı — 2026-11-15

**Temel kural:** Harita ve rota cihazda; internet yalnızca isteğe bağlı geliştirmeler ve güncellemeler içindir. Bağlantı kaybolduğunda aktif rota değişmez/sıfırlanmaz.

## Çalışma senaryoları

| Durum | Uygulama |
| --- | --- |
| İnternet yok, harita kurulu | Harita/rota cihazdan; çevrimiçi eklentiler kapalı |
| İnternet var, harita kurulu | Yerel harita/rota korunur; izinli veri varsa çevrimiçi ek özelliklere geçiş |
| Mobil veri | Büyük harita indirmesi otomatik başlamaz |
| Wi-Fi/ölçüsüz ağ | Sürüm kontrolü uygun; büyük dosya ancak kullanıcı izin verdiyse |
| İnternet sürüşte kesildi | Ekrandaki rota ve yerel navigasyon devam etmeli |
| Harita henüz indirilmedi | İlk kurulumda internet indirmesi veya dosya/USB ile aktarım gerekir |

## V0.2 geliştirici önizlemesi — kodlanan

- Android ConnectivityManager varsayılan ağ izleme: INTERNET + VALIDATED + NOT_METERED.
- OfflineFirstPolicy saf Kotlin karar motoru ve 7 JVM testi.
- İnterneti otomatik kullan ayarı ve durum ekranı; tercih cihazda tutulur.
- Yerel Türkiye MBTiles dosyasını veri fabrikası manifestiyle SHA-256 üzerinden doğrulama taslağı; doğrulama arka planda yapılır.
- Android minSdk 26, targetSdk 35; diğer uygulamalardan bağımsız app.json; PUBLIC kaynak ve PRIVATE imzalama.

## Henüz yapılmamış

- Türkiye MBTiles paketinin gerçek derlenmesi ve Android MapLibre katmanına bağlanması.
- Çevrimdışı Valhalla rota grafiği, GPS ile yol yönlendirme ve sesli manevralar.
- Cloudflare R2 alanı / ilk indirme / bölünmüş indirme devam ettirme / otomatik güncelleme çalışan servisi.
- Gerçek çevrimiçi trafik, kamera, yol kapanışı gibi veriler: izinli, güvenilir ve ücretsiz kaynak bulunmadan vaat edilmez.

## Sonraki sistem

Kullanıcı başlatmış büyük indirime Android 14+ UIDT Job (API34); küçük, ertelenebilir Wi-Fi güncellemelerine WorkManager ve ağ kısıtları. Her pakette SHA-256 kontrolü, önceki haritayı otomatik silmeden güvenli aktif sürüm değişimi. Cloudflare kullanılamıyorsa offline harita ve USB import çalışmalı. Kullanıcı konumunu internete göndermeden, yalnız anonim sürüm manifesti ile güncelleme kontrolü.

Kaynak: https://developer.android.com/develop/connectivity/network-ops/reading-network-state
Kaynak: https://developer.android.com/develop/background-work/background-tasks/data-transfer-options
