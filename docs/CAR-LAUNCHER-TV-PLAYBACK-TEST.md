# CAR LAUNCHER V2.1 — TV Oynatma Engeli Düzeltmesi

Tarih: 11 Kasım 2026
Değişiklik dalı: `fix/carlauncher-tv-playback-v2-1-1`

Mevcut CAR LAUNCHER V2.1 ekranları, siyah-altın arayüzü, logo, radyo, sürüş haritası, kısayollar, widgetlar ve alt menü korundu.

## Kod değişiklikleri

- `apps/carlauncher/v2/WorldMedia.kt`: TV oynatımında `parkedVerified` ve `speedKmh` kontrolleri kaldırıldı; seçili kanal varsa oynatıcı açılıyor. TV penceresindeki “TV yalnızca park hâlindeyken izlenebilir” uyarısı kaldırıldı. Kanal listesi, yerinde görüntüleme, oynatıcı kontrolleri ve kapatma aynen devam ediyor.
- `apps/carlauncher/v2/apply_v2.py`: Sadece test sürüm numarası `versionCode 4`/`1.2.2-tv-playback-test` ve mevcut V2.1'i silmeden kurabilmek için debug paket son eki `.v21tvtest` olarak değiştirildi.
- `.github/workflows/06-carlauncher-tv.yml`: Aynı ücretsiz Linux Actions sürecini kullanarak ayrı test APK üretimi.

## Gerçek derleme ve paket doğrulaması

- GitHub Actions: https://github.com/okiminsozu-droid/RASASER-FREE/actions/runs/42963705494
- Sonuç: **SUCCESS**
- Artifact ID: `13830317416`, `RaSaSeR-CAR-LAUNCHER-V2-1-TV-TEST-APK`
- APK: `app-debug.apk`, 11.997.588 bayt
- APK SHA-256: `b66a9c39f7e0853c4c7f27ffe31b609951d5d129395bf0955792fc0b5e13f007`
- Manifest, DEX, Launcher ikon görseli ve ZIP bütünlüğü kontrol edildi.
- Geçerli V2.1 uygulamaya dokunulmaması için test paket adı: `com.rasaser.launcher.v21tvtest`

**Cihaz testi henüz yapılmadı.** İçerikteki bazı internet TV kanallarının erişilebilirliği, yayıncı izinleri ve oynatıcı biçimi değişebilir. TV'nin sürücü tarafından seyir esnasında izlenmesi tehlikelidir; kullanım trafik güvenliği ve geçerli mevzuata uygun olmalıdır.

PRIVATE RASASER deposu değiştirilmemiştir. Başarılı V1/V2/V2.1 sürümleri korunmuştur.
