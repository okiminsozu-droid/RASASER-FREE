# RaSaSeR Widget Studio — Özgün geliştirme planı

## V0.1.0 — Bu dalda kaydedilen ilk kaynak sürüm
- Bağımsız Android uygulama (minSdk 26, targetSdk 35).
- Siyah/altın tasarım, ana ekran ve widget düzenleyici.
- Her widget için ayrı resim / etiket / görünürlük / hedef.
- Uygulama listesi üzerinden seçim; uygulama, tarayıcı, arama ekranı, not, sistem ayarları ve el feneri.
- İsteğe bağlı fotoğrafın yerel olarak küçültülerek saklanması; widget silinince verinin temizlenmesi.
- Uygulama izni: isteğe bağlı CAMERA dışında gerekmez. Internet/Storage/Contact izinleri yok.
- Android widget ana ekranda yeniden boyutlandırılabilir; elle veya sistem üzerinden düzenlenebilir.
- **Durum:** Yalnız kaynak sürüm; APK/AAB derleme ve cihaz testleri yapılmadı.

## V0.2.0 — Tasarım atölyesi
1. Önceden hazırlanmış özgün, lisans sorunu olmayan vektör simgeler; özel renk, çerçeve ve köşe tipleri.
2. Widget'ların canlı ön izlemesi, fotoğraf kırpma/hizalama ve açık/koyu temalar.
3. Çevrimdışı profil şablonları; birden fazla widget tasarımını hızlı oluşturma.
4. Erişilebilirlik: büyük yazı, yüksek kontrast, TalkBack açıklamaları.

## V0.3.0 — Gelişmiş cihaz araçları
1. Kamera, galeri, takvim, alarm, dosya yönetimi ve hesap makinesi için sistem Intent kısayolları (uyumluluk testi sonrası).
2. Metin notları için basit düzenleme, renkli not widget'ı.
3. Widget ayarlarını kullanıcı onayıyla JSON olarak dışa aktarma/içe aktarma (Storage Access Framework, özel veri sızıntısını önleyen uyarılar).
4. 6 dil: Türkçe, İngilizce, Almanca, İspanyolca, Arapça, Japonca.

## V1.0.0 — Yayına hazırlık kalite kapıları
- Gerçek fiziksel Android 8–17 cihazlarında ve yaygın launcher'larda test.
- Erişilebilirlik, ekran döndürme, 1x1–4x4 widget boyutları, yeniden açma, sistem yeniden başlatma.
- İzin reddi, bozuk görsel, iptal edilen dosya seçimi, çok büyük resim, widget silme.
- Kullanıcı onayı → GitHub Actions debug derlemesi → bağımsız test → PRIVATE imzalı release.
- Google Play için gizlilik politikası, Data safety, ekran görüntüleri ve mağaza metinlerinin hazırlanması.
- Vaat edilen her özellik cihaz testi geçmeden tamamlandı olarak işaretlenmez.

## Teknik dayanak
Android resmi AppWidgetProvider/RemoteViews API, resize callback ve Storage Access Framework. Üçüncü taraf uygulamanın kodu/markası/görseli alınmaz.
