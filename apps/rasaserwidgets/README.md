# VARYOK — RaSaSeR • APPS & TECHNOLOGY (0.3.3 TEST)

Android / RaSaSeR FREE bağımsız modülü:
`apps/rasaserwidgets/`, paket `com.rasaser.free.rasaserwidgets`.
Diğer uygulamalar, web sitesi ve Cloudflare değiştirilmedi.

## Kullanıcı akışı
- Pembe-pudra ana ekran, kullanıcı onaylı siyah-altın VARYOK logosu, 6 dil, tek **Widget Oluştur** butonu.
- Telefonda başlatılabilir uygulamalar, kendi ikonlarıyla alfabetik listelenir; yeni kurulum/kaldırmalar ekrana yeniden girildiğinde veya paket değişikliği bildirildiğinde görünür.
- Bir uygulama seçilince gerçek ikonu, adı, varsayılan/özel fotoğraf ve %0–100 görünürlük kaydırıcısı ile önizleme açılır.
- **Oluştur** ile Android başlatıcısına widget ekleme isteği gönderilir. Android onay verip yeni widget kimliğini oluşturduğunda özel bir **mutable PendingIntent** callback tetiklenir. Callback gerçek widget kimliğine `kind=app`, uygulama paketi, başlık ve opaklığı kaydeder. Varsa özel fotoğrafı geçici dosyadan o widget'a aktarır ve `RemoteViews` görünümünü günceller.
- Widget'ın tamamına dokunmak özel, uygulama-içi kısayol yönlendiricisini çağırır; yönlendirici kaydedilen paket için başlatma Intent'ini bulup uygulamayı açar. İkon ile kullanıcı fotoğrafı aynı dokunma eylemine sahiptir.
- Android'in widget pinleme yöntemi **WidgetEditorActivity'yi otomatik açmaz**. Önceki 0.3.2 kodundaki görselin görünmemesi / dokunmanın çalışmamasının kök nedeni buydu.
- Önceki sürümden kalan **yapılandırılmamış widget'lar** artık görünmez yerine VARYOK simgesini gösterir ve VARYOK'u açar. Doğru hedefi yüklemek için eski boş widget'ı ana ekrandan kaldırıp yeniden oluşturmak gerekir; kullanıcı seçimi bilinmeyen widget otomatik onarılamaz.

## Ayarlar kaldırıldı
Ana ekrandaki sağ üst **ayarlar ikonu**, `SettingsActivity` ve gereksiz silme ekranı kaldırıldı. Bayraklı dil seçimi sağ üstte, gizlilik ⓘ simgesi ve `www.okiminsozu.com` ana sayfa altındadır.

## Değişmeyen kurallar
- Offline-first, internetsiz widget oluşturma; reklam / takip / analitik SDK ve INTERNET izni yok.
- Fotoğraflar, widget etiket/hedef/görünürlük ayarları ve ücretsiz hak sayacı **yalnız cihazda** tutulur; kullanıcı hesabı ve sunucu kayıt sistemi yok.
- İlk üç başarıyla kaydedilmiş widget ücretsiz. Silmek hak iadesi sağlamaz. Gerçek Premium / Play Billing henüz bağlı değildir.
- Fotoğraf seçimi Android sistem seçicisi üzerinden, arka planda ve boyut sınırıyla yapılır.
- Diğer sistem widget işlevleri, mevcut `WidgetEditorActivity` üzerinden korunmuştur.
- Test APK'sı **debug** imzalıdır; Play Store üretim sürümü değildir. Gerçek cihaz ve farklı başlatıcılarda işlevsel test ayrıca gereklidir.

## Test
`python3 apps/rasaserwidgets/qa/test_source_contract.py` kaynak sözleşmelerini;
GitHub Actions `VARYOK Test APK` Gradle `:app:assembleDebug` ve unit testleri çalıştırır.
