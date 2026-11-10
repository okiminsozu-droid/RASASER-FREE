# PUBLIC geliştirme ve PRIVATE motor entegrasyonu

Ana özel kaynak: `okiminsozu-droid/RASASER` deposunun `RaSaSeR/` Kotlin/JVM motor platformu ve adaptörleri. Güncel motor iskeletinin gerçek, doğrulanmış ve sürümlenmiş Android AAR entegrasyonu henüz kurulmamıştır. Public arayüzler gerçek motor değildir.

## Güvenli süreç
1. FREE'de yeni uygulamanın yalnız yayımlanabilir kodu `apps/<slug>/` altında geliştirilir ve test edilir.
2. PUBLIC GitHub Actions yalnız imzasız, özel motor içermeyen debug/test çıktıları üretir.
3. PRIVATE üretim, bakımcının açıkça onayladığı **sabit public commit SHA**'sını kullanır; değişken branch HEAD'i körü körüne indirmez.
4. PRIVATE motorun doğru sürümü ve gerçek Android adaptörleri eklenip yeniden derlenir. Mevcut Kotlin/JVM çekirdeği hazır Android AAR olarak kabul edilmez.
5. PRIVATE ortamda birim, entegrasyon, cihaz, izin, performans ve release testleri geçilir. Bu aşama private Actions kotasını kullanabilir veya güvenli yerel makinede çalıştırılabilir.
6. Her uygulama için ayrı Google Play **upload signing key** ile optimize release AAB imzalanır. Özel anahtarlar Github koduna, Action loglarına, artifact'lara veya pull request'lere yazılmaz.
7. İmzalı çıktı, SHA-256 özeti, sürüm ve kullanılan public commit/private motor sürümü kayıt altına alınarak PRIVATE release arşivine ve ayrı şifreli yedeğe alınır.
8. Play Console'da uygulama testleri ve App Signing doğrulanarak yayınlanır.

## Kesin güvenlik sınırları
- PUBLIC iş akışına PRIVATE depo veya private package erişimli token, deploy key, secret, keystore yahut hizmet hesabı verilmez.
- PUBLIC derlemeye özel AAR dahi indirilmez: bytecode ve içerik APK içinden çıkarılabilir.
- `pull_request_target` ile güvenilmeyen fork kodları ve secret'lar birlikte çalıştırılmaz.
- Var olan uygulamaların dosyaları ve kararlı sürümleri otomatik değiştirilmez.
- PRIVATE arşivde SHA, applicationId, versionCode ve anahtar yedeğinin durumu saklanır. Özel imza anahtarları yine ayrıca korunur.

## Henüz yapılmayan işlem
Private tarafta salt yeni motor-release köprüsü ve arşiv otomasyonu, ilk gerçek uygulama için paket kimliği ve motor sürümü belirlendiğinde ayrı ve kontrollü olarak oluşturulacaktır. Şu anda burada PRIVATE erişim otomatik kurulmamıştır.
