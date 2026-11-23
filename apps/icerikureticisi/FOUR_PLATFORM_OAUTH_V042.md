# RaSaSeR İçerik Üreticisi 0.4.2 — Facebook, Instagram, X, YouTube

## Beklenen sonuç
Kullanıcı her platformdan çok sayıda ayrı hesabı bir defa resmî yetki ekranında onaylayacak. Yetkiler cihazda şifreli tutulacak, süre bitiminde sağlayıcının izin verdiği ölçüde otomatik yenilenecek ve kullanıcı yeniden uygulama seçmeyecek. İptal edilmiş izni uygulama otomatik geri alamaz.

## Bu geliştirme dalında uygulananlar
1. Ana bağlantı sırası Facebook, Instagram, X (Twitter), YouTube; eski diğer platformlar korunuyor. X'in gerçek logo vektörü APK'da.
2. X ve eski Twitter profil URL'leri resmî alan adı denetimi altında; başka site taklitleri reddedilir.
3. DeviceOAuthVault: sağlayıcı kimliği, kullanıcı hesabı, izinler, son doğrulama, token süresi, şifreli access/refresh token için Android Keystore AES-256-GCM hesabı başına bağımsız kasa. Dış sunucuda kullanıcı verisi yok. Uygulama içinde sabit hesap sayısı limiti yok.
4. OAuthPkceCore: X native public OAuth istemcisi için RFC 7636 S256 PKCE, 256 bit state, offline.access izni içeren URL hazırlığı; bunlar JUnit testleriyle denetleniyor.
5. Dört platformdan ancak gerçekten OAuth ile doğrulanmış hesapların listelenebileceği ayrı yönetim paneli, oturum süresi/yeniden yetki uyarısı ve cihazdan kaldırma. Üretici profili silinirse kasa silinir.
6. Mevcut Premium, 15 günlük deneme, PIN, biyometri, Telegram/Bluesky doğrudan bağlantıları, eski profil kısayolları ve internet sitesi aynen kalıyor.

## AÇIKLAMA: Dört platformda canlı OAuth ve otomatik yayın henüz YOK
Henüz Facebook, Instagram, X ve YouTube için geçerli geliştirici uygulama kimliği, onaylanmış izin veya kayıtlı callback bulunmuyor. Bu nedenle token alınmış, yetki yenilenmiş veya otomatik paylaşım yapılmış gibi göstermek yanıltıcı olur. Eklenen kasa/PKCE modülü gerçek bağlantıya hazırlıktır.

| Platform | Üretime geçmek için şart |
| --- | --- |
| Facebook Sayfaları | Meta Developer App, Pages yayın izinleri, uygulama incelemesi |
| Instagram Professional | Meta Developer App, Instagram Business/Creator yayın yetkileri, inceleme; kişisel hesap desteği API'yle aynı değildir |
| X | X Developer Native App Client ID, kayıtlı birebir callback, API kotası ve ücretli olabilen kullanım kredileri |
| YouTube | Google Cloud OAuth Android kimliği, SHA imza kaydı ve youtube.upload kapsamı; kanal erişimi gerekir |

Özel OAuth client secret gerektiren bir sağlayıcının sırrı APK içine konulamaz. Kullanıcı şifresini istemek resmî OAuth yetkilendirmesi yerine geçmez. Birinci taraf sunucu bulundurmama şartı ile bazı sağlayıcı API işlemleri bir arada desteklenmeyebilir.

## QA
- Sekiz resmî platform URL/icon testi, X URL taklidi reddi, aynı platformdan çoklu kayıt.
- PKCE S256 RFC test vektörü, güçlü rastgele state/verifier, invalid client/redirect reddi.
- Android build + JUnit.
- Gerçek dört platform OAuth ve API yayın uçtan uca testleri geliştirici kimlikleri sağlanmadığı için yapılmadı.

## Resmî kaynaklar
Meta: https://developers.facebook.com/docs/instagram-platform/
Facebook Pages: https://developers.facebook.com/docs/graph-api/reference/page/feed/
X PKCE: https://docs.x.com/fundamentals/authentication/oauth-2-0/authorization-code
X API credits: https://docs.x.com/x-api/getting-started/pricing
Google OAuth: https://developers.google.com/youtube/data/guides/authentication
