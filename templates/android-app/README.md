# Android uygulama şablonu

Uygulama oluşturma komutu (depo kökünden):
`python3 tools/new_app.py ornekuygulama "Örnek Uygulama"`

Android 15 (targetSdk 35), minSdk 26, Kotlin 2.0.21, Jetpack Compose başlangıç iskeletidir. PUBLIC Actions yalnız debug/test üretir. Gizli motorlu gerçek release PRIVATE ortamda tamamlanmalıdır.

Şablonda uygulama içi **Ayarlar > Lisanslar** butonu vardır. Lisans metni `app/src/main/assets/legal/licenses.txt` dosyasından okunur. Gerçek bağımlılıklar için yayın öncesi düzeltilmelidir. `app.json` kimlikleri, paket ve güvenli imzalama kuralını korur.
