# RaSaSeR İçerik Üreticisi — Bağımsız 7 Platform Bağlantı Motoru (v0.3.8)

## Kullanıcının hedefi
Buffer veya abonelik isteyen sosyal medya yönetim araçları kullanılmadan, Instagram, Facebook Sayfaları, YouTube, TikTok, Pinterest, Bluesky ve Telegram kanallarının RaSaSeR İçerik Üreticisi üzerinden ayrı ayrı bağlanması; kullanıcının izin verdiği hesaplara yayın, hesapların yeniden giriş yapmadan yönetilmesi ve gerçek yayın sonuçlarının gösterilmesi.

## 0.3.8'de gerçekten uygulananlar
- Eski `apps/icerikureticisi` üzerinde ayrı geliştirme dalı; eski stabil sürüm korunur.
- Sosyal platform kataloğuna Pinterest, Bluesky, Telegram eklendi. Profil URL kaydetme OAuth izni değildir.
- **Telegram Bot API (ilk canlı hedef)**: BotFather tokenı ve `@kanaladi` / `-100...` ID girilerek `getMe`, `getChat`, `getChatMember` doğrulaması; bot kanalda yayın yönetici yetkisine sahipse bağlantı kaydı. Yalnız `channel` türü hedefler desteklenir.
- **Telegram çoklu kanal**: Her içerik üreticisine birçok ayrı bot/kanal bağlantısı; listeden seçim, açık kullanıcı onayıyla `sendMessage` veya `sendPhoto`; `message_id` döndüğünde gerçekten yayınlandı bilgisi.
- **Gizlilik**: Bot tokenı AES-GCM ile AndroidKeyStore anahtarına bağlı, yalnız telefonda ve uygulama özel alanında şifreli depolanır. Hiçbir kullanıcı parola/anahtarı GitHub'a, siteye veya Cloudflare'e konmaz. Kimlik kasası geri yüklenemiyorsa kayıtlar sessizce sıfırlanmaz.
- **Kaldırma**: Tekil kanal tokenı silinir; içerik üreticisi kaldırılınca ona ait bütün Telegram tokenları da silinir. Uygulamanın `allowBackup=false` koruması korunur.
- **Fotoğraflar**: Kullanıcının seçtiği JPG/PNG/WEBP fotoğrafı (10 MB üst sınırı) doğrudan Telegram API'ye gönderilir; başka sunucuya yüklenmez. Telegram sınırlarının değişmesi hâlinde güncel belgeler kontrol edilmelidir.
- **Eski özellikler**: PIN, parmak izi, 15 günlük deneme, Premium promosyon sistemi, çevrimdışı profil kayıtları ve manuel sosyal paylaşım aynen kalır. İlişkisiz uygulamalara ve `www.okiminsozu.com` sitesine dokunulmaz.

## Telegram bağlantısı için test adımları
1. Telegram'da resmî `@BotFather` hesabından bot oluşturun ve API tokenını alın. Token bir parola gibi gizlidir.
2. Botu yönetmek istediğiniz **kanala** ekleyin ve **mesaj yayınlama** yönetici izni verin.
3. İçerik Üreticisi içinde üretici profilini açın → **Bağımsız hesap bağlantıları** → **Telegram kanalı bağla**.
4. Tokenı ve `@kanaladi` veya `-100...` kanal ID'sini girin. **Doğrula ve kaydet** seçeneğine basın. İzin verilmemişse kayıt yapılmaz.
5. Bağlı kanalları işaretleyin, metin ve isteğe bağlı fotoğraf seçin, **Seçili kanallarda yayınla** düğmesine basın. Her kanalın kendi sonucu ve Telegram'ın verdiği `message_id` ayrı gösterilir.
6. Bağlantı kaldırıldığında token yerel güvenli kasadan silinir. BotFather'dan token iptal etmek ayrı bir işlemdir.

**Açık sınır**: Telegram bu yolda bir kişisel kullanıcı OAuth hesabı değil, kanal üzerinde izinli bir bot bağlantısıdır. Kanal postları gönderilebilir; Telegram mesaj kutusunun tüm bildirimlerini toplamak için bir izin verilmiş sayılmaz. Telefon kapalıyken zamanlanmış sunucu yayını henüz yoktur.

## Diğer altı platform — gereken resmî üretim bağlantıları
| Platform | Gerçek yayın için hazırlık |
| --- | --- |
| Bluesky | AT Protocol OAuth; yayınlanan istemci metadata URL'si, PKCE, PAR, DPoP ve uygun DID/scope oturum saklama. Klasik OAuth2 token alma akışı tek başına yetmez |
| Instagram | Meta geliştirici uygulaması; desteklenen Professional Business/Creator hesapları ve yayın kapsamı; ilgili Meta ürün/değerlendirme |
| Facebook | Meta OAuth, kullanıcının yönettiği **Sayfalar** için ilgili Pages izinleri ve onay |
| YouTube | Google Cloud OAuth2 istemcisi, `youtube.upload` gibi gerekli kapsamlar, doğrulama ve kota |
| TikTok | TikTok Login Kit, Content Posting API scopes ve doğrudan yayın onayları, platform incelemeleri |
| Pinterest | Pinterest OAuth2 app id, access/refresh token işlemleri, Pin creation izinleri |

- Bu altı platform için **Canlı OAuth, token yenileme veya otomatik yayın henüz yoktur**. İnternette profil sayfasının açılması hesap sahipliğini doğrulamaz.
- `apps/icerikureticisi/independent-connect/schema.sql` önceki sürümde **yayımlanmamış** Cloudflare D1 bağımsız backend şeması olarak bulunur. Doğrulanmış platform OAuth yapılandırmaları, güvenli Worker endpoint'leri ve client credentials temin edilmeden deploy edilmez.
- Sunucu tabanlı OAuth'nun ileride kullanılacağı hizmette tokenlar ayrı anahtarla AES-GCM şifrelenir, D1 kaydı kanallara göre ayrılır; istemciye refresh token verilmez. Dinamik PKCE/DPoP, state/nonce, rotasyon, iptal, idempotent yayın sonucu kontrolü gereklidir.
- Aynı platformda sınırsız **yerel kayıt** yapılabilir; API'ların kota ve hesap/uygulama doğrulama şartları bunun dışında kalır. Otomatik yayın platform izin ve iş kuralları ile sınırlıdır.

## Denetim ve yayına çıkış
- `assembleDebug` + `testDebugUnitTest` kaynak testleri; gerçek Telegram bot tokenı repoya yazılarak test edilmez.
- Gerçek cihaz + gerçek test kanalı üzerinde `getMe/getChat/getChatMember/sendMessage/sendPhoto` uçtan uca testi yapılmadan üretim için tamamlandı denilmez.
- Dışa aktarılan debugging APK test kimliği `com.rasaser.free.icerikureticisi.v038` ve kaynak sürümü **0.3.8 / versionCode 12**. Eski imzalı kurulum silinmez; ayrı sürümler arasında hesaplar otomatik kopyalanmaz.
- Sunucu/mağaza uyumluluğu ve varsa ücretli servis maliyetleri üretimden önce ayrıca kontrol edilir.

## Referanslar
- Telegram resmî Bot API: https://core.telegram.org/bots/api
- AT Protocol OAuth: https://atproto.com/guides/about-oauth
- Instagram: https://developers.facebook.com/docs/instagram-platform/
- Facebook Graph: https://developers.facebook.com/docs/graph-api/
- YouTube: https://developers.google.com/youtube/v3/
- TikTok: https://developers.tiktok.com/
- Pinterest: https://developers.pinterest.com/docs/api/v5/
