# RaSaSeR FREE — Herkese Açık Android Uygulama Fabrikası

Bu PUBLIC depo, yeni RaSaSeR uygulamalarını standart ücretsiz GitHub Actions Linux çalıştırıcılarında geliştirmek ve test etmek içindir.

**KORUMA:** [RASASER](https://github.com/okiminsozu-droid/RASASER) ana deposu PRIVATE kalır; mevcut uygulamalar ve motorlar buraya kopyalanmaz. Özel anahtar, parola, token veya motor kaynak kodu burada bulunmaz.

## Yeni uygulama
1. `python3 tools/new_app.py uygulamaadi "Uygulama Adı"` çalıştırın.
2. `apps/uygulamaadi/` klasöründe uygulamayı geliştirin.
3. Kodu bu depoya gönderin. Güvenlik ve testler Actions'ta çalışır.
4. **Actions > Android FREE Derleme > Run workflow** ekranında `app_slug` alanına `uygulamaadi` yazın.
5. Özel motor gerektiren son sürümü güvenli PRIVATE ortamda sabit public commit SHA'sından yeniden üretin, orada imzalayın, şifreli güvenli arşive alın ve Play Console'a yükleyin.

## İçerik
- `apps/` : yalnızca yeni ve bağımsız uygulamalar
- `templates/android-app/` : Kotlin + Compose, Android 15 uyumlu başlangıç şablonu ve uygulama içi Lisanslar ekranı
- `engine-contracts/` : yalnız açık arayüz sözleşmeleri, gerçek gizli motor içermez
- `tools/` : proje oluşturma, sürüm/yol denetimi, güvenlik testleri
- `.github/workflows/` : public, anahtarsız kontrol ve debug derlemeleri
- `docs/PRIVATE-ENTEGRASYON.md` : PRIVATE motorla güvenli son entegrasyon
- `docs/YAYIN-KONTROL-LISTESI.md` : imzalama, arşiv ve Play yayın kontrolleri

## Güvenlik ve maliyet
PUBLIC Actions hiçbir şekilde PRIVATE depoyu klonlamaz, özel paket çekmez ve imza anahtarı kullanmaz. Public derleme çıktıları yalnız public kodu içerebilir. Standart public Linux runner dakikaları ücretsiz olsa da concurrency, iş süresi, artifact depolama ve kötüye kullanım sınırları vardır. PRIVATE son derleme bu ücretsiz public dakikalardan yararlanmaz. Public repo herkesin kaynak kodu inceleyebileceği anlamına gelir.

## Lisans
Ücretsiz dağıtım, kaynağa otomatik açık kaynak lisansı tanımaz. Bu depoya genel bir açık kaynak lisansı henüz verilmemiştir. Her uygulamanın ayrı lisans bildirimi ve üçüncü taraf uyumluluğu yayın öncesi gözden geçirilir.

**Durum:** FREE iskeleti kuruldu; henüz uygulama release'i, imzalı AAB veya Google Play yayını yoktur.
