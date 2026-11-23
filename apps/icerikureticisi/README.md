## v0.3.9 — Hesap açılışında tekrar uygulama seçilmesini azaltma

- Ekrandaki hesap avatarına, hesap adına veya `Uygulama ›` düğmesine dokunulduğunda artık web `ACTION_VIEW` bağlantısı yerine `SocialIntents.openAccount` çağrılır. Android'de kurulu sosyal medya uygulamasının **belirli ana launcher bileşeni** doğrudan açılır.
- `Uygulama kısayolunu kaydet ve aç` da aynı yolu kullanır. Böylece daha önceki sürümde oluşan `web bağlantısı → Android seçici → Instagram` tekrarı çoğu tek kurulumda kaldırılır.
- Sadece `Site ›` resmî web profilini tarayıcıda açar; kurulu sosyal uygulama bulunamadığında kayıtlı profil URL'si güvenli tarayıcı yoluna gider.
- Önemli Android/OEM sınırı: Vivo `Ⅱ·Instagram` gibi **klon** uygulamalarda sosyal medya paylaşım niyeti Android'in ayrı kullanıcı/profil seçicisini açabilir. Üçüncü taraf uygulamalar profil korumalarını atlayamaz. İşletim sisteminde varsa **varsayılan klon/main Instagram** tercihi ayrı ayarlanabilir. `ACTION_SEND` ile belirli Instagram oturumundaki hesaba direkt otomatik post göndermek mümkün değildir.
- Eski kayıtlar korunur, SQLite şeması değişmez. *Uygulama kısayolunun kaydedilmesi resmî OAuth veya yayınlama yetkisi değildir.* Gerçek OAuth hesap bağlantısı Meta/TikTok/Google vb. geliştirici kayıt ve onaylarıyla ayrı yapılmalıdır.
- v0.3.9 / versionCode 13; ayrı debug kimliği `com.rasaser.free.icerikureticisi.v039`. `www.okiminsozu.com`, mevcut site ve başka uygulamalar değiştirilmedi.

# RaSaSeR İçerik Üreticisi — 0.3.4 Resmî Profil Bağlantıları ve Görseller

**Kapsam:** Yalnızca `apps/icerikureticisi/`, ayrı `feature/icerik-ureticisi-biometric-trial-cloudflare-v031` dalı. Çalışan 0.2.0 sürümü ve önceki tüm Android projeleri değiştirilmez.

## v0.3.7 — Kısayol kaydı ve resmî tarayıcı düzeltmesi

- `Telefondaki uygulamaya bağla ve kaydet`: kullanıcı adı boş olsa da platform için ayrı **yerel uygulama kısayolu** kaydedilir; sonra kurulu uygulama açılır. Kullanıcı adı veya resmî profil URL'si varsa hesap profili olarak kaydedilir. Kısayol herhangi bir hesap yetkisi vermez.
- `Resmî siteye bağla ve kaydet`: profil boşsa platformun resmî ana sitesinin web kısayolunu kaydeder; profil URL'si veya kullanıcı adı varsa belirli profil bağlantısını kaydeder. Android'in `FLAG_ACTIVITY_REQUIRE_NON_BROWSER` bayrağı tarayıcıları reddettiği için kaldırıldı; bilinen **tarayıcı paketleri** (Chrome, Vivo Browser, Firefox vb.) doğrudan denenir, gerekirse Android seçim penceresi açılır.
- Hesap kartı ana sayfa kısayollarını `Kısayol kaydedildi • Hesap yetkisi yok` şeklinde doğru etiketler; resmî URL bağlı görünen bir OAuth hesabı değildir. Aynı platformdan istediğiniz kadar ayrı kısayol eklenebilir.
- Premium, PIN, biyometri, 15 günlük deneme, veri tabanı v5, mevcut hesaplar ve manuel paylaşım korunur. **Cloudflare promosyon servisi dışında hiçbir sunucu, okiminsozu.com veya başka uygulama değiştirilmez.**
- Kaynak sürüm 0.3.7 / versionCode 11. Yan yana kurulan test APK kimliği `com.rasaser.free.icerikureticisi.v037`; önceki test kopyalarının verilerini otomatik aktarmaz.
- Gerçek otomatik gönderim ve bağlantı yetkilendirmesi, ilgili platformların resmî OAuth uygulama kimlikleri, gerekli izinleri ve güvenli sunucu callback akışı kurulmadan devreye alınmamıştır.
- Test: `OfficialSocialLinksTest` kısayol sınıflandırması ve önceki URL güvenlik testleri; `assembleDebug` ve `testDebugUnitTest` GitHub Actions.


## 0.3.4 resmî profil adresleri ve büyük görseller (23 Kasım 2026)

- Mevcut `CreatorAvatar` görselleri ana profil kartında **48 → 76 dp**, profil ekranında **42 → 64 dp** oldu. Uzun/dar fotoğraflar `ContentScale.Crop` ile alana sığacak şekilde görünür.
- Android SQLite hesap tablosunda **platform başına veya toplam hesaba sabit bir sayı limiti zaten yoktu**. Bu sürüm aynı Instagram/Facebook/TikTok/YouTube platformunda çok sayıda farklı profil kaydetmeyi açık ve anlaşılır biçimde desteklemeye devam eder. Her kayıt bağımsız hesap etiketi ve HTTPS profil linki tutar.
- Her bilinen platform için **"Resmî siteyi aç"** kontrolü eklendi. Kullanıcı resmî siteden kendi profil adresini alıp kaydedebilir. Adresin **gerçek platform alan adına** ait olduğu, başka alan adlarının taklidi olmadığı kontrol edilir. Önceden kaydedilmiş manuel hesaplar ve başka platformlar silinmez.
- Hesap kartındaki resmî profil bağlantısı doğrudan açılabilir; **"API yetkisi yok"** ibaresi bunun bir OAuth bağlantısı olmadığını netleştirir. Tek bir Android Instagram paketi içindeki çok sayıda kullanıcı oturumu veya uygulama klonu `ACTION_SEND` üzerinden ayrı ayrı kontrol edilemez.
- **Otomatik yayın veya resmî hesap bildirimleri bu sürümde başlatılmadı.** Instagram için Meta geliştirici uygulaması ve Business/Creator erişimi; Facebook için Pages izinleri; TikTok için onaylı Direct Post; YouTube için Google Cloud OAuth+Data API gerekir. Her kullanıcı ve her platform kimliği için ayrı OAuth izin akışı ve platforma göre yayın durumu sorgulaması/webhook gerekir. Platform bildirimleri genel sosyal uygulama mesaj kutusunun sınırsız kopyası değildir; API'nin desteklediği olaylarla sınırlıdır.
- Güvenlik gereği hiçbir hesap parolası, OAuth erişim belirteci, kullanıcı bildirimi veya gizli API anahtarı uygulamaya/Cloudflare'e alınmaz. Resmî servis hesapları ve gerekli OAuth istemci kimlikleri henüz yapılandırılmadı. **Sadece profil linki ile otomatik gönderim / doğrulanmış 'Gönderildi' bildirimi üretmek mümkün değildir.**
- Deneme süresi, PIN/parmak izi, Premium kodu ve başka uygulamalar korunur. Test kimliği `com.rasaser.free.icerikureticisi.v034` olarak ayrı kurulabilir; test hesaplarını otomatik olarak önceki imzalı uygulamadan alamaz.
- **Sürüm:** 0.3.4 (versionCode 8). API onayları tamamlanmadan gerçek otomatik yayın vaat edilmez.

## 0.3.2 düzeltmeleri (23 Kasım 2026)
- İçerik profili açılınca kaydedilmiş hesaplar **varsayılan olarak seçili**; kullanıcı istediği hesapların işaretini kaldırabilir. Yeni eklenen hesap da otomatik işaretlenir.
- Kaydedilen uygulama için **doğrudan başlatma intenti** öncelenir. Mevcut `ACTION_SEND` medya paylaşımı uygun uygulamaya açık bileşen üzerinden aktarılır. **Telefonun uygulama klonlama sistemi** (örneğin Vivo'nun `Ⅱ·Instagram` seçicisi) farklı kullanıcı profilleri açtığından Android'in sistem seçim penceresi her durumda otomatik atlanamayabilir.
- Paylaşım uygulaması açılınca üzerine **“Bilgi” uyarısı bindirilmez**. Uygulamaya aktarılan içerik için yerel durum görüntülenir. Kullanıcı gerçek yayını platform uygulamasında tamamladıktan sonra **“Gönderildi olarak işaretle”** diyebilir. Bu, platform onayı değil **kullanıcının manuel teyididir**; sahte başarı bildirimi gösterilmez.
- Profil bağlantısı kaydetmek, otomatik paylaşım yetkisi sağlamak anlamına gelmez. Instagram/Facebook/YouTube/TikTok'ta gerçek hesap bazlı yayın ve platform onaylı gönderim bildirimi **ancak resmi OAuth, yayın API yetkileri, developer uygulaması ve uygun platform incelemeleriyle** mümkündür. Bu sürümde otomatik API yayını yapılmaz.
- Premium üst ikonu standart emoji yerine **özgün siyah-altın vektör taç** tasarımına çevrildi. Premium hakkı varsa aktif durumu gösterir.
- 15 günlük deneme dolunca uygulama girişinde **Premium ekranı doğrudan açılır**; yeni Premium etkinleştirmesi ancak Cloudflare'de doğrulanan promosyon koduyla mümkündür. İçerikler silinmez. Doğrulama sonrasında ekran yenilenerek tam erişim açılır.
- **Google Play ücretli satın alma**: Satış için bu uygulamanın Play Console'a eklenmesi, ürün/abonelik ve resmi Billing Library + güvenli sunucu satın alma onayı gerekir. Henüz yapılandırılmadığından satın alma düğmesi gibi yanıltıcı bir kontrol gösterilmez.
- Yeni kaynak sürüm **0.3.2 (versionCode 6)**. Debug paket kimliği `com.rasaser.free.icerikureticisi.v030` olarak tutulur; farklı test imzaları yüzünden yükseltme zorunlu olarak başarılı olmayabilir. Önceki kurulum kaldırılmamalıdır.

## 0.3.1 düzenlemeleri (23 Kasım 2026)
- **Telefonun yerel parmak izi:** PIN oluşturulunca biyometrik geçiş belirginleşir; parmak izini etkinleştirmek için Android BiometricPrompt üzerinden gerçekten başarılı cihaz doğrulaması istenir. PIN güvenli yedek olarak kalır. Parmak izi görüntüsü/şablonu uygulama tarafından alınmaz veya saklanmaz.
- **Üst ve alt hizalama:** Üst kontrol ikonları ekran genişliğine dengeli yerleştirilir; Android güvenli ekran alanları (durum çubuğu, gezinme çubuğu, çentik) dikkate alınır. Alt marka ve site yazısı telefon gezinme çubuğunun üstünde ortalanır.
- **15 gün ücretsiz deneme:** İlk açılışta başlayan çevrimdışı tam erişim. Keystore HMAC ile mühürlenen yerel başlangıç/son görüntüleme saati tutulur. Saati geriye alarak uzatmaya karşı temel koruma vardır. Süre bitince işlemler Premium ekranı açılana kadar engellenir; yerel içerikler otomatik silinmez. Çevrimdışı cihazdaki deneme takibi tam anlamıyla manipülasyona dayanıklı lisans sunucusu değildir.
- **Promosyon kodu / Cloudflare:** Mevcut, kullanıcı verilerinden bağımsız `rasaser-bagimsiz-kullanici-girisi` Worker ve D1 `owner_allowed_packages` izin listesine hem `com.rasaser.free.icerikureticisi` hem `com.rasaser.free.icerikureticisi.v030` paketleri yetkilendirilmiştir. Kod APK'ya, GitHub'a veya uygulama tercih dosyasına yazılmaz; ilk etkinleştirme için internet gerekir. Kod yalnızca sunucudan doğrulanır.
- **Sürüm:** source 0.3.1 / versionCode 5. Yan yana kurulan debug paket kimliği `.v030` sabit bırakılmıştır; mevcut test verilerini koruyan güncelleme yolu amaçlanır.
- **Henüz yapılmayanlar:** Google Play Billing ürün, abonelik ve satın alma altyapısı yoktur. "Google Play'de aç" yalnız RaSaSeR araması açar; satın alma veya deneme sonrası otomatik tahsilat yapmaz. Üretim yayınına geçmeden önce gerçek bir satın alma yolu ve mağaza uyumluluğu hazırlanmalıdır. Android cihazda biyometrik/promosyon uçtan uca test ve APK derlemesi henüz çalıştırılmamıştır. GitHub Actions maliyetine sebep olmamak için otomatik derleme yeni dalda tetiklenmemiştir.

## Yeni eklenenler
- İçerik üreticisi profili oluştururken galeriden fotoğraf/logo; var olan profile görsel değiştirme. Görsel SQLite'ta yalnız dosya yolu ve `filesDir` içinde özel küçük PNG olarak tutulur. Liste kartındaki varsayılan numara yerine profil görseli görünür. SQLite v3→v4 veri silmeyen migration.
- 🇹🇷 Türkçe, 🇬🇧 İngilizce, 🇩🇪 Almanca, 🇪🇸 İspanyolca, 🇯🇵 Japonca, 🇸🇦 Arapça dil açılır menüsü; bilinen arayüz etiketleri yerel çeviri tablosundan değişir. Kullanıcının yazdığı profil/hesap adları yanlış otomatik çevrilmez; profiller için 🌐 menüsünden her dile isteğe bağlı karşılık girilebilir. Bilinmeyen dinamik metin kaynak dilde kalabilir.
- Krem/açık tema ve antrasit/koyu tema seçimi; cihazda kalıcı tercih.
- Sol üst Premium simgesi, Google Play'de RaSaSeR araması ve ortak RaSaSeR promosyon kodu doğrulama alanı. Google Play satış/abonelik entegrasyonu **yoktur**, mağazada yayımlanmamış bir uygulama için uydurma ürün bağlantısı kullanılmaz.
- Premium kodu APK'ya veya GitHub'a konmaz. Halihazırdaki Cloudflare `/v1/promo/redeem` servisi üzerinden **HTTPS ve sunucuda D1 whitelist** kontrolü; `owner_lifetime` yanıtı alınırsa cihazdaki hak Android Keystore HMAC ile mühürlenir. İlk etkinleştirme internet gerektirir. İzin listesine bu paketin normal ve v030 test kimlikleri 23 Kasım 2026'da eklenmiştir; doğrulama başarısızsa Premium hakkı verilmez.
- Üst çubukta 🔐 PIN oluştur/değiştir + parmak izi aç/kapat. PIN 6–12 rakam, rastgele tuzla PBKDF2-HMAC-SHA256 (180.000 yineleme), beş yanlış denemede 30 sn kilit. Güçlü parmak izi destekli cihazda kullanıcı onayıyla BiometricPrompt; PIN yedeği her zaman var. PIN kurulduktan sonra sonraki uygulama açılışı kilitli başlar; başarıyla kimlik doğrulanınca uygulama otomatik açılır.
- Ana sayfa altında `RaSaSeR APPS & TECHNOLOGY` + tıklanabilir `www.okiminsozu.com`.
- Uygulama ikonunda özgün krem, altın ve siyah klaket/kamera çizimi; tüm ekranlarda ölçeklenebilir vektör kaynak.

## Korunan özellikler
Sınırsız içerik profili/sosyal medya kısayolları, Instagram/Facebook/YouTube/TikTok ve özel platformlar, resmî uygulamayı doğrudan açma, fotoğraf/video seçip açıklama yazma, hesap kutucukları/Hepsini seç, sırayla paylaşım aktarımı, WorkManager ile zamanlanmış **hatırlatma**.

**Önemli:** Zamanlanan medya resmî sosyal medyaya kendi kendine yüklenmez. Yayın onayı kullanıcının ilgili platform uygulamasında verilir. Instagram/Facebook/YouTube/TikTok paylaşımları için platform OAuth/API yetkileri olmadan gerçek, otomatik, hesap bazlı yayın yapılmaz.

## Güvenlik ve uyumluluk
- `android.permission.INTERNET` sadece Premium HTTPS doğrulaması ve tarayıcı/site açılışları için gerekir; profiller, PIN ve medya verileri buluta yüklenmez.
- `USE_BIOMETRIC` Android normal iznidir. Parmak izi kullanımı sistemin açık onay ekranıyla gerçekleşir. Başka uygulamaların bildirimlerini okuyan NotificationListenerService yoktur.
- Galeriden fotoğraf seçimi Android sistem seçicisiyle; geniş depolama izni verilmez.
- İlk açılıştaki yayın hatırlatması bildirim izni Android 13+ üzerinde kullanıcı onayıyla istenir. Sistem izinleri otomatik onaylanamaz.
- PIN sıfırlama güvenlik nedeniyle kolaylaştırılmadı; hem PIN hem biyometrik erişim kaybolursa uygulama verilerini silmeden kurtarma mekanizması yoktur.
- Debug APK `com.rasaser.free.icerikureticisi.v030` paket kimliğiyle mevcut 0.1.1/0.2.0 testlerini silmeden ayrı kurulur. Bu yüzden hesaplar yeni test sürümüne otomatik taşınmaz. Google Play üretim sürümünün imza/yükseltme stratejisi ayrıca gerekir.
- Ortak promosyon kodu sunucuda doğrulanmadan Premium etkin sayılmaz. **Diğer uygulamalara otomatik entegrasyon yapılmaz.** Kod tek olsa bile uygulama bazında API entegrasyonu gerekir.
- Logoda RaSaSeR marka dışı tescilli sosyal platform ikonlarının yetkileri ayrı değerlendirilmelidir.

## Derleme
```bash
gradle --no-daemon --project-dir apps/icerikureticisi :app:assembleDebug :app:testDebugUnitTest
```
Derlemeden önce sürüm ve en az bir kimlik doğrulama testi doğrulanmalıdır. GitHub Actions iş akışı yeni dala özel koşacak, hassas NotificationListenerService bulunmadığını ve debug kimliğinin `.v030` olduğunu kontrol edecek.

Kaynaklar: AndroidX Biometric, Android Keystore, Android per-app language, Google Play Billing promo ve Cloudflare Workers resmî belgeleri.
