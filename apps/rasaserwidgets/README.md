# VARYOK — RaSaSeR • APPS & TECHNOLOGY (0.3.4 TEST)

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
