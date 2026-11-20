# VARYOK — RaSaSeR • APPS & TECHNOLOGY (0.4.0 OWNER ACCESS PREVIEW)

**Yeni sürüm 0.4.0 / versionCode 9 / Android API 36.** Bu sürüm
RaSaSeR markasına 7 kez dokununca açılacak Google ile merkezi sahip
doğrulama modülünün kaynaklarını içerir. Yalnız Google üzerinden
doğrulanmış **okiminsozu@gmail.com** hesabına Cloudflare sunucusunun
imzaladığı cihaz+paket bağlı `owner_lifetime` belgesi verilebilir.

**ŞU ANDA SAHİP PREMİUM ETKİN DEĞİL.** Cloudflare Web OAuth Client Secret
ve **tek sunucu ECDSA-P256 özel imzalama anahtarı** kurulmamıştır.
Bu nedenle APK'daki yalnızca **genel** doğrulama anahtarı alanı
boştur ve uygulama güvenli biçimde erişimi reddeder. Hiçbir
özel anahtar, Google Client Secret, PIN, keystore veya yükleme imzası
`RASASER-FREE` public deposuna kaydedilmeyecek.

**Mevcut işlevler korunur:** Üç farklı uygulama ücretsiz; aynı
uygulamaya sınırsız tekrar widget, gerçek app ikonları/özel fotoğraf,
şeffaflık, düzenleme, uygulama açma, PIN/parmak izi kilidi,
6 dil, Premium bilgilendirmesi, site/gizlilik bağlantısı.
Sahiplik sertifikası geçerli olduğunda üç farklı uygulama limiti
uygulanmaz. Normal ücretsiz kullanıcıların kotası değişmez.

**Veri ilkesi:** Widget bilgileri/fotoğraflar cihazdan çıkmaz. Yalnız
sahip girişini başlatan kullanıcı, Google hesabı kimliği ve cihazın
**açık anahtarını** merkezi Cloudflare doğrulama servisi üzerinden
doğrulatır. Google parolası, PIN, fotoğraf veya widget içeriği
sunucuya gönderilmez. `INTERNET` izni yalnız isteğe bağlı sahip
doğrulaması için eklendi; takip/reklam/analitik SDK yok.

**Yayın kuralları:** 31 Ağustos 2026'dan itibaren yeni Android
telefon sürümleri için hedef API 36. Tek başına `bundleRelease`
çıktısı **İMZASIZ AAB**'dir; Play'e gönderilemez.
Gerçek Google Play yüklemesi için ayrı ve kalıcı **upload signing key**
gerekir. Mevcut kullanıcıların güncellemelerinin anahtarı
değiştirilmez. Bu anahtar uygulama içi sahiplik belgesi imzalama
anahtarından tamamen farklıdır.

**Google Play politikası:** Normal kullanıcının görmediği 7
dokunuşlu sahip girişi, Play inceleyicisinden saklanmayacak;
uygulama erişim talimatlarında açıkça belgelendirilmeli. Gizli/
belgesiz işlev yasağı incelenmeden ve Google OAuth + gerçek cihaz
testleri geçilmeden üretime yüklenmeyecek.

Play Console API bağlantısı bu konuşma sırasında VARYOK paketine
erişemiyor (`credentialsConfigured=false`, başka uygulama paketi
kayıtlı). Play'e yükleme ve yayınlama henüz yapılmadı.

## Önceki sürüm notları

Mevcut Android uygulaması **sadece** `apps/rasaserwidgets/` klasöründe geliştirilir.
Paket kimliği `com.rasaser.free.rasaserwidgets`, kullanıcının seçtiği
siyah-altın logo ve pembe/pudra ana sayfa korunur.

## Üç farklı uygulama ücretsiz
Ücretsiz sınır, toplam widget sayısı değil, widget oluşturulmuş **üç ayrı uygulama
paketidir**. Örnek: YouTube için 5 widget ve Google Maps için 3 widget oluşturmak
toplam 2 farklı uygulama hakkı kullanır. Üçüncü farklı uygulama da ücretsizdir;
dördüncü farklı uygulama için Premium gerekir. Önceden ücretsiz hak kullanılmış
uygulamaya yeniden widget eklemek ücretsiz olmaya devam eder. Dördüncü farklı
uygulama için gerçek ödeme modülü henüz bağlı değildir.

Sürüm 0.3.3 ve öncesindeki `total_created` sayacı, tekrarları ayırt etmediğinden
artık ücretsiz hakkı bloke etmez. v0.3.4 ilk açıldığında **halen cihaz içinde
kayıtlı, başarıyla oluşturulmuş widget'ların** farklı uygulama paketlerini
kullanarak eski sayaçtan tek seferlik geçiş yapılır. Eskiden silinen widget'ların
paketleri tutulmamış olduğundan onlar güvenilir biçimde geri çıkarılamaz;
geçiş yalnız erişilebilen yerel kayıtlardan yapılır. Yeni sürümde kullanılan
farklı paketlerin yerel listesi silinmiş widget'tan bağımsız tutulur.

## Bir defalık Şifre Oluştur butonu
Ana sayfada **Widget Oluştur** altında **Şifre Oluştur** görünür. Bir kez PIN
veya biyometrik doğrulama etkinleşince buton artık görünmez.

- **PIN oluştur**: 4–12 rakamlık PIN + ikinci doğrulama. Sadece tuzlanmış,
  PBKDF2-HMAC-SHA256 ile türetilmiş değer yerel özel ayarlara kaydedilir;
  PIN düz metin halinde saklanmaz.
- **Parmak izi**: Android 9+ sistemin kendi `BiometricPrompt` ekranında,
  telefonda **önceden kayıtlı parmak izi/biyometri** ile doğrulanır. Uygulama
  parmak izi kaydı oluşturmaz ve parmak izi görüntüsü/şablonu görmez veya tutmaz.
  Cihazın güvenlik ayarlarında parmak izi kayıtlı değilse kullanıcı PIN
  oluşturabilir. Cihaz kilidi yedeği, biyometrik yöntem artık kullanılamıyorsa
  Android'in cihaz kimlik bilgisi ekranına yönlendirir.
- Kilit açık olduğunda uygulama açılışında doğrulama istenir. Aynı oturumda
  kısa süreli kullanım için 3 dakikalık doğrulama penceresi bulunur.
- İlgili güvenlik metinleri Türkçe, İngilizce, Almanca, İspanyolca, Japonca ve
  Arapça olarak yerel kaynaklarda bulunur.

Bu, VARYOK uygulama arayüzü giriş kilididir. Android'in ana ekranındaki
zaten oluşturulmuş kısayollar, diğer uygulamaları açmaya devam eder;
bir dosya kasası veya tam disk şifreleme sistemi değildir.

## Diğer işlevler korunur
Uygulama ikonlarıyla kaydırılabilir arama listesi; fotoğraflı özelleştirme
ve %0–100 görünürlük; gerçek widget kimliğine kaydetme ve tıklayınca hedef
uygulamayı açma; 6 dil, çevrimdışı çalışma, reklam/takip/analitik olmaması;
alt tarafta www.okiminsozu.com ve gizlilik ⓘ ikonu; ayarlar simgesinin olmaması.

## Test ve yayın
Bu bir test/debug sürümüdür, Play Store yayın imzası kullanılmaz.
Derleme ve statik kaynak testleri Android cihaz davranışını tam olarak
doğrulamaz. Testte: 1) Önceden 3 aynı uygulamanın hakkı tüketmesi hatası,
2) üç farklı uygulamadan sonra dördüncünün engellenmesi,
3) aynı uygulamanın tekrar eklenmesi,
4) PIN/parmak izi kurulumundan sonra butonun gizlenmesi,
5) yeniden açılışta doğrulama ve ana ekrandaki kısayol davranışı kontrol edilir.

Uygulamayı kaldırmak uygulama-içi PIN'i, farklı paket sayacını ve yerel widget
verilerini de silebilir. Güncellemede mümkünse aynı test imzasını koruyun.

## 0.3.5 — Premium bilgilendirme simgesi
- Ana ekran sol üstte **altın renkli taç ve altında Premium**; sağ üstteki bayraklı dil seçici, VARYOK logosu, Widget Oluştur, bir defalık Şifre Oluştur, ücretsiz hak göstergesi ve alt site/gizlilik bağlantısı korunur.
- Premium simgesine dokununca altı dilde bilgi penceresi açılır: **99 TL/yıl Google Play Billing ile otomatik yenileme planı**, aktif abonelikte farklı uygulamalar için sınırsız widget hakkı, abonelik bitince yeni Premium widget oluşturmanın durması ve eski widget'ların çalışmaya devam etmesi.
- Türkçe, İngilizce, Almanca, İspanyolca, Japonca ve Arapça metinler çevrimdışı yerel dosyada bulunur; dil değiştiğinde pencere seçili dili kullanır.
- **Ödeme ve abonelik hakkı doğrulaması bu sürümde etkin değildir.** Premium ekranı açıkça bunun bir bilgilendirme olduğunu bildirir, kullanıcıyı ödeme yapılıyormuş gibi yanıltmaz, ödeme butonu göstermez. Türkiye 99 TL/yıl tutarı bir ürün planıdır; canlı fiyat ve diğer ülke para birimleri gelecekte Google Play ürün ayrıntılarından alınmalıdır.
- Gerçek satın alma için Google Play Console'da VARYOK paketine yıllık otomatik yenilenen abonelik ürünü oluşturulması, Play Billing Library entegrasyonu, satın alma doğrulaması, abonelik durum yönetimi ve eski widget'ların korunması gerekir. Bu aşama mevcut test APK'sında yapılmamıştır.
