# RaSaSeR Navigasyon: Cloudflare R2 ülke paketi v1

Bu çalışma yalnız `apps/navigasyon` kapsamındadır. R2 bucket: `rasaser-navigasyon-turkiye`. Test alanı: `https://pub-e5b8f3a2a9e84a9dbfc1c01ca1442264.r2.dev/v1/`.

- 606 MB Türkiye vektör haritası: `v1/rasaser-turkiye-shortbread.zip` (605992151 bytes; SHA-256 c7d8b277a916ffd4ec7839c7c0420104c19b88fcfcea456be7078346003bacfc).
- 1.15 GB Türkiye Valhalla çevrimdışı rota: `v1/valhalla_tiles.tar` (1148661760 bytes; SHA-256 bdfd9a67d8fdd0d5856013e889c6e48cd8a14560934cc5d2b6f3c16a8f6a554f).
- 66 MB Türkiye yer adları: `v1/turkey-places.zip` (66446365 bytes; SHA-256 048f474b1ff427ac2310b5201a283d027d87ec769ca7e808254ffbbdf6b603f0).

Üç veri arşivi toplam **1.821.100.276 bayt** (yaklaşık 1,82 GB; 1,70 GiB). Uygulama kullanıcı onayıyla Wi-Fi üzerinden sırayla indirir; geçmişte doğrulanmış yerel paketler bozulursa silinmez. R2 dosyası indirilmezse aynı SHA ile GitHub Release varlığına bir kez geri döner.

## R2 yüklemek

GitHub Actions secrets alanına Cloudflare R2 **bucket ile sınırlı** S3 erişimi olan `R2_ACCESS_KEY_ID` ve `R2_SECRET_ACCESS_KEY` değerlerini özel olarak ekleyin. Bunlar hiçbir zaman herkese açık kaynak dosyalara veya konuşmaya yazılmamalı. Ardından **Navigasyon Turkiye R2 Paketlerini Elle Yayinla** iş akışını `workflow_dispatch` ile, yalnız kullanıcı onayıyla başlatın. Bu iş akışı 3 varlığı GitHub Releases'dan çeker; SHA-256 doğrular; S3 multipart ile R2'ye gönderir ve dosya uzunluklarını kontrol eder.

**Not:** Cloudflare'ın genel R2 REST nesne yükleme yöntemi 300 MB ile sınırlandığı için 606 MB ve 1,15 GB dosyalar doğrudan bu yöntemle yüklenemez. S3 multipart gereklidir. Bu yapı R2 test alanı `r2.dev` ile çalışır; üretim ölçeğinde Cloudflare özel alan adı önerir, mevcut web sitesinin DNS'ine dokunulmaz. Uygulama ayrıca tüm arşivleri cihazda SHA-256 ile doğrular.

**Henüz tamamlanmamış:** R2 bucket gerçek büyük verileri barındırana ve Android cihaz üzerinde uçtan uca indirme/çevrimdışı rota testi geçene kadar genel dağıtım yapılmamalıdır. R2 kotası ücretsiz sınırların içinde kalırsa ek R2 depolama bedeli çıkmaz; kullanım her ay sınırlar üzerinden yeniden değerlendirilmelidir.
