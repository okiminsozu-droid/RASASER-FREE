# RaSaSeR İçerik Üreticisi — Bağımsız Sosyal Hesap Bağlantı Motoru

## Karar
Buffer, Ayrı bir sosyal medya otomasyon şirketi, ücretli aracılık API'si veya platform şifresi gerektiren uygulama **kullanılmaz**. RaSaSeR'ın kendi Android istemcisi + kendine ait Cloudflare Worker API'si + isteğe bağlı D1/R2/Queues ile yalnız resmi sosyal ağ uç noktalarına bağlanılır. Bu dizin yeni sistemin tasarım ve güvenlik sözleşmesidir; bu committe canlı OAuth servisi yayımlanmamıştır.

Mevcut 0.3.4 projesi bozulmaz. Sosyal hesap profili URL'si yalnız yer imidir. `ChannelAuthState.CONNECTED` durumuna **ancak** resmi platform OAuth kodu yetkili sunucuda değiştirilmiş, token doğrulanmış, gerçek platform kimliği ve kapsamlar okunmuş ise geçilebilir.

## Kapsam ve sınırlar
- Her içerik profili için sınırsız yerel kanal kaydı; aynı platformdan N farklı hesap. Teknik limitler platform API ve hizmet kotası, telefonun depolaması ve sağlayıcı izinleriyle sınırlıdır.
- Hesap bağlama: kullanıcının resmi sistem tarayıcısında/güvenilir OAuth ekranında izin vermesi; uygulama kullanıcı parolasını almaz.
- Kullanıcı yetkiyi kaldırmadıkça ve token yenilenebildiği sürece oturum korunur. Yenilenemiyorsa `REAUTH_REQUIRED`. "Asla tekrar giriş gerekmez" garantisi yoktur.
- Her `(installation, provider, provider_channel_id)` ayrı kanal; Instagram klon uygulaması/Android gönderim seçicisi OAuth kimliği sayılmaz.
- Gönderim durumu: `DRAFT`, `QUEUED`, `UPLOADING`, `PROCESSING`, `PUBLISHED`, `FAILED`, `REAUTH_REQUIRED`, `MANUAL_ACTION_REQUIRED`. `PUBLISHED` yalnız gerçek platform kimliği ve doğrulanmış yayın sonucu varsa.
- Bildirimler: yalnız platformların izin verdiği webhook/okuma olayları; Instagram/Facebook/TikTok/YouTube'un kullanıcı cihazındaki tüm bildirimlerinin doğrudan kopyalanacağı iddia edilemez.

## Platform izin ve entegrasyon hazırlıkları
| Platform | Resmi OAuth ürün | Otomatik yayın şartı | Başlangıç |
|---|---|---|---|
| Instagram | Meta Instagram API with Instagram Login | Professional (Business/Creator), `instagram_business_basic`, `instagram_business_content_publish`; Meta değerlendirmesi | https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/ |
| Facebook | Meta Facebook Login for Business + Graph Pages API | Kullanıcının yönettiği Sayfa, `pages_show_list`, `pages_manage_posts`, `pages_read_engagement` ve gerekli inceleme | https://developers.facebook.com/docs/graph-api/reference/page/ |
| TikTok | TikTok Login Kit + Content Posting API | Gerekli onaylar, `video.publish` gibi ilgili scopes, paylaşım limitleri | https://developers.tiktok.com/docs/en/login-kit-overview |
| YouTube | Google OAuth 2.0 + YouTube Data API | `https://www.googleapis.com/auth/youtube.upload`, gereken proje ve uygulama incelemeleri, kota | https://developers.google.com/youtube/data/guides/authentication |

**Not:** Sosyal platformların izinleri, ürünleri ve API sürümleri değişebilir. Üretime geçmeden her platform için yeni sürüm ve dokümantasyon doğrulanır.

## Android istemci / sunucu ayrımı
1. Android cihazda `AndroidKeyStore` EC-P256 anahtarı oluşturulur. Kurulum kimliği + public key güvenli servise kaydedilir; istemci istekleri nonce/timestamp/mesaj imzası ile yetkilendirilir. Uzun ömürlü secrets ve refresh tokenlar APK'ya veya SQLite'a yazılmaz.
2. Kullanıcı `Resmî hesabı bağla` dediğinde Worker `POST /v1/connections/start` isteğini yalnız doğrulanmış kurulumdan kabul eder. Parametreler: `provider`, `local_creator_id`, `local_account_id`.
3. Worker rastgele, tek seferlik `state`, son kullanma süresi, provider tipine uygun PKCE saklar. Meta/TikTok/Google istemci kimliklerini Worker secrets/vars içinde yönetir; kullanıcıyı resmi platform yetkilendirme URL'sine yönlendirir.
4. OAuth callback Worker HTTPS alan adına gelir. Worker **state**, expiry, PKCE ve consent scopes kontrol eder; tek kullanımlık code'u sunucu tarafında tokena çevirir. Provider API üzerinden gerçek kullanıcı/sayfa/kanal ID'sini doğrular. Yetki yoksa hata kaydeder; bağlı görünümü açmaz.
5. D1 bağlantı metadatası; tokenlar AES-GCM ile ayrı anahtar/IV kullanılarak şifrelenmiş olarak saklanır. Anahtar yalnız Cloudflare Worker secret'tır; günlüklerde, URL'de veya yedekte plaintext token olmaz. Token yenileme planlı olarak sunucuda gerçekleştirilir.
6. Uygulama `GET /v1/connections` isteğiyle yalnız **kendi** kurulumuna ait kanal kimliği, etiket, kapsam ve bağlantı durumunu okur. Doğrulanmış durumu lokal SQLite v5'e yansıtır. Android'e OAuth token döndürülmez.
7. `POST /v1/publish/jobs`: gönderi sahibi, seçili kanal kimlikleri, medya türü ve bir defalık işlem kimliği ile idempotent çalışır. Platform limitlerine uygun sıra/kuyruk/yükleme vardır. Yayın tamamlandığını yalnız API yanıtı ve son durum sorgusu doğrular.
8. `DELETE /v1/connections/{id}`: provider token revoke (sağlanan ürün varsa), kanal token silme, yerel durum sıfırlama. Kullanıcı açıkça isterse D1/R2 içerik temizliği ve veri silme süreci.
9. Bulut medya R2'ye yüklenirse kısa saklama süresi, erişimi kısıtlı signed URL, kapalı erişim ve sonrasında otomatik silme politikası şarttır. Kullanıcıya açıklanmadan medya kalıcı buluta yüklenmez.
10. Telefondan bağımsız zamanlı gönderim için Worker Cron/Queues/Workflows gibi servislerin maliyeti ve Cloudflare plan kısıtları teyit edilir. İşlem ücretli plana zorlanmamalıdır; ücretsiz limit yetmiyorsa açıkça belirtilir.

## Güvenlik ve politika kalite kapıları
- OAuth `state` tahmin edilemez, kısa ömürlü ve tek kullanımlık; PKCE provider desteğine göre zorunlu; redirect URI exact match; dışarıdan gelen URL ile open redirect yok.
- HTTPS dışında yetki akışı yok. Kimlik bilgileri, cookie'ler, access tokenlar, refresh tokenlar loglara veya repo dosyalarına yazılmaz.
- Kurulum kendi kanalını görür; bir kullanıcının başka kullanıcı adına kanal bağlayıp post atması engellenir.
- Kullanıcı istemeden içerik otomatik yayınlanmaz: yayın yetkisi verilmiş olsa da içerik ve hedef kanallar kullanıcının onayıyla seçilir.
- Rate limiting, CSRF koruması, imza replay koruması, bağlantı kopması, yetki iptali, kaldırma ve platform 429 senaryoları test edilir.
- Ücretlendirme: Buffer aboneliği yok. Ancak platform kota/inceleme gereksinimleri ve Cloudflare tüketimi sıfır maliyet garantisi değildir.
- Mevcut Premium promosyon Worker'ı ile **aynı servis/anahtar/veritabanı** kullanılmaz. `www.okiminsozu.com` ve diğer uygulamalar değiştirilmez.

## Mevcut ilerleme
- Android `OfficialAccountAuthorization.kt` ile resmi/manuel bağlantı durumu ve izne göre yayın kontrolü eklendi.
- `CreatorDatabase` SQLite 4→5 **yerinde, veri silmeyen** geçiş: `auth_state`, `provider_channel_id`, `connection_id`, `last_verified_at` sütunları. Tüm önceki hesaplar `manual` olarak korunur.
- Bu aşama **çekirdek ve veri sözleşmesidir**; gerçek OAuth bağlantıları, yayınlama API'leri, otomatik bildirim işleme **henüz devrede değildir**.
- Eksik dış bağımlılıklar: RaSaSeR'a ait Meta Developer, TikTok Developers ve Google Cloud projelerinde uygulama kaydı, sunucu alan adı ve OAuth Redirect URI onayları, platforma özel gerekli yayın izinleri. Bunlar olmadan gerçek yetki akışı başlatılmaz.

## İlerideki API şeması (henüz canlı endpoint yok)
```text
POST   /v1/installations/register
POST   /v1/connections/start
GET    /oauth/{provider}/callback
GET    /v1/connections
DELETE /v1/connections/{connection_id}
POST   /v1/publish/jobs
GET    /v1/publish/jobs/{id}
GET    /v1/events?channel_id=...
```

Tasarım tercihi: `Worker OAuth Engine` ve `Android Client Connect UI` iki farklı sürümlü modül. Credentials eksikken kullanıcıyı sahte login'e veya sosyal medya şifresi isteyen herhangi bir form'a yönlendirmeyin.
