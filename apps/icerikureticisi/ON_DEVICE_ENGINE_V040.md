# RaSaSeR İçerik Üreticisi 0.4.0 — Telefonda Bağımsız Bağlantı Motoru

## Kesin mimari
- Bütün uygulama, bağlantı istemcileri, içerik veritabanı, kullanıcı arayüzü ve güvenli yetki kasaları **APK ile telefona kurulur**.
- RaSaSeR Cloudflare Worker, D1, R2, okiminsozu.com, Buffer, Postiz veya diğer ara sosyal medya yönetim hizmetine hiçbir otomatik ağ isteği yapılmaz.
- Sosyal medya yayınları için, **yalnızca kullanıcının açıkça başlattığı** bağlantı/yayın sırasında ilgili sosyal ağın **kendi sunucusuyla doğrudan HTTPS** iletişimi kurulur. Ağ bağlantısı olmadan uzak platformda yayın yapılamaz.
- Hesap adları, profil görselleri, yerel kısayollar ve taslaklar Android SQLite/dosyalarda; Telegram bot tokenları ve Bluesky JWT'leri **Android Keystore AES-256-GCM** ile şifreli yalnız cihazda. Geliştiriciye hesabın veya tokenın kopyası iletilmez, otomatik telemetri/analitik SDK yoktur.
- Önceden açılmış Premium cihaz içi HMAC makbuzları korunur. Yeni Premium **ECDSA P-256 (SHA256withECDSA)** ile internetsiz doğrulanır. APK'ya yalnız açık doğrulama anahtarı konur. İmzalama **özel anahtarı** cihaz APK'sına veya GitHub'a girmez. Yeni çevrimdışı lisans kodunu uygulama telefon içinde kontrol eder; başvuruya ait kimlik/siteye gönderim yok.
- `CreatorFooter` marka adresi artık tıklanabilir link değildir.
- Yeni debug test paketi `com.rasaser.free.icerikureticisi.v040` ile yan yana kurulacaktır; eski uygulamanın verisi silinmez. Debug APK imzası stabil olmadığı için eski paketin üstüne kurulmamalıdır.

## Gerçek entegrasyonlar
| Platform | v0.4.0 | Teknik akış |
| --- | --- | --- |
| Telegram **kanalları** | Gerçek bağlantı ve tek/çoklu kanala metin+fotoğraf | BotFather bot tokenı + kanala yayın yetkili yönetici atanması; `getMe/getChat/getChatMember` ile kontrol; `sendMessage/sendPhoto` onay ID |
| Bluesky (bsky.social üzerinde barınan hesaplar) | Gerçek ayrı hesap bağlantısı, metin+fotoğraf | Kullanıcının ayarlardan oluşturduğu **ayrı App Password**, doğrudan `https://bsky.social/xrpc` createSession; Android Keystore JWT şifreli; refreshSession; createRecord, uploadBlob; dönen AT URI onay |
| Instagram | Mevcut resmî uygulamaya manuel paylaşım | Instagram Professional API için Meta app ID, yayın izinleri ve secret gereksinimleri; salt APK'da secret saklanamaz |
| Facebook Pages | Manuel paylaşım | Facebook Login for Android mobil kimliği ve Pages izinleri için Meta geliştirici kaydı/app review |
| YouTube | Manuel paylaşım | Android-native Google OAuth mümkündür ancak resmi Google Cloud OAuth client / onaylı `youtube.upload` kapsamı ve yayın kotası kayıtları gerekir |
| TikTok | Manuel paylaşım | Login Kit kodu **server-side** tokena çevirmeyi gerektiriyor; kullanıcı koşuluna göre bu otomatik yol kapalı |
| Pinterest | Manuel paylaşım | OAuth client secret token alışverişinde gerekli; gizli anahtar APK içine gömülmeyecek |

**Önemli:** Bluesky uygulama parolası **OAuth değildir**. Resmî ATProto OAuth üretim kullanımında HTTPS istemci metadata URL'si bekler. "Kullanıcıya ait/başka siteye bağlı olmayan APK" koşuluna uygun pratik doğrudan erişim için yalnız **uygulama parolası** yolu kullanılmıştır; ana parola kabul edilmez. PDS'leri farklı olan tüm ATProto sunucuları bu ilk sürümde kapsanmıyor.

## Hesaplar ve toplu paylaşım
- Aynı Telegram botuyla veya farklı botlarla birden fazla kanalı yönetmek ve birden fazla Bluesky hesabı bağlamak mümkün; yerel hesap sayısı için kodda sabit limit yok, telefon depolaması ve platform API limitleri var.
- İçerik oluşturucu başına bağımsız seçim listesi; seçilen Telegram kanalları + Bluesky hesaplarına tek düğmeyle **doğrudan** fotoğraf/metin gönderilir. Kullanıcı yayın düğmesine basmadıkça hiçbir içerik yayınlanmaz.
- Bluesky metni güvenli olarak en fazla 300 Unicode kod noktası ve 3000 UTF-8 baytla sınırlandırılır; gönderilecek görsel telefonda sıkıştırılır, 950 KB'ı geçmez. AT URI dönmediyse yayına "başarılı" denmez. Telegram `message_id` doğrulaması kullanılır.
- Bağlantı kaldırma ve üretici profili silmede ilgili şifrelenmiş tokenları da cihazdan silme uygulanır. Sosyal ağın kendi ayarlarından token/bot yetkisi iptal etmek ayrı işlemdir.
- Zamanlama eski yöntemle **telefon bildirimi** olarak çalışır; telefon kapalıyken sunucusuz planlı otomatik gönderim garanti edilmez. Diğer beş platform için yalnız manuel `ACTION_SEND` akışı vardır; gerçek yayın onayını uygulama alamaz.

## Premium gizlilik dönüşümü
- Önceki sürümde "promosyon kodunu sunucuya gönder" vardı. Bu bağımsız sürümde HTTP istek kodu tamamen kaldırılmıştır; mevcut kodun aynı şekilde geçerli kalması garanti değildir. Yeni biçim `RSRL1.payload.signature` (owner lifetime).
- İmzalama aracı ve **özel anahtar** yalnız sahibine ayrı güvenli dosya olarak verilir. Ortak koda erişen herkes aynı evrensel kodu kullanabilir; tek cihaz/tek hesap sınırlaması server olmadan güvenilir sağlanamaz. Ancak uygulama code signing özel anahtarını asla taşımaz.
- İstendiği gibi eski 15 günlük deneme / PIN / biyometrik / önceki aktif Premium makbuzları korunur.
- Test paketine Cloudflare'de yeni yetki eklenmez.

## Güvenlik denetimi
- Her API yalnız sabit sağlayıcı endpointine gider. Kullanıcının kontrolündeki URL'ye access token eklenmez, endpoint redirection kapalıdır; ağ HTTPS.
- Bluetooth, çağrı kayıtları, telefon rehberi, sosyal medya uygulama bildirimleri okunmaz. SMS veya şifre tarama yok.
- Yazılımsal aygıt kompromisi/root düzeyindeki saldırıya karşı Android Keystore sınırlamalarını aşıp tam güvenlik garanti edilemez.
- Gerçek hesap bilgisi/kanal olmadan **canlı Telegram/Bluesky testleri yapılmış sayılmaz**; kodun Android derlemesi ve JUnit testleri ayrıca kontrol edilir.

## Referanslar
- Telegram: https://core.telegram.org/bots/api
- Bluesky auth: https://atproto.com/guides/sdk-auth
- Bluesky posts: https://bsky.network/docs/about-bluesky-content/posts/
- ATProto OAuth: https://atproto.com/specs/oauth
- TikTok Login Kit: https://developers.tiktok.com/docs/en/login-kit-overview
- Pinterest authentication: https://developers.pinterest.com/docs/getting-started/set-up-authentication-and-authorization/
- YouTube Android: https://developers.google.com/youtube/data/quickstart/android
