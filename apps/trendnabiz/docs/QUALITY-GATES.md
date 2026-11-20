# TRENDNABIZ test and release gates

- [x] Separate public GitHub source folder; no modification to other apps, site, R2 maps or existing Cloudflare Workers
- [x] Distinct Cloudflare Worker and cron created; execution source has no news database writes
- [x] Native Android foreground WebSocket consumer and transient 180-second in-memory display coded
- [x] Existing Android persistent news cache and previous Python/SQLite server removed in this branch
- [x] Android debug compilation + JVM unit test workflow successful (run 44795358502); artifact TRENDNABIZ-0.3.0-TEST-APK
- [ ] Physical-device test of new 0.3.0 build
- [ ] WebSocket event reception + 3-minute expiry + 5 languages
- [ ] Long-running/locked/background notification delivery with genuine Android permissions and sustainable power behavior
- [ ] Press notification on lock-screen: source and actions work; no fabricated content
- [ ] Cloudflare Worker platform log/privacy verification, no news content in logs
- [x] Public HTTPS /healthz and /v1/probe verified externally, Durable Object WS handshake exercised; every-minute Cron trigger configuration checked
- [ ] Live event ingestion and end-to-end notification delivery performance measured
- [ ] Strong source licensing and origin consistency for each additional publisher feed
- [ ] World-scale concurrency, rate limits, abuse handling, Cloudflare free/paid cap analysis
- [ ] Google Play billing, subscriber verification and compliant Privacy Policy
- [ ] Store/production approval (only isolated owner-requested debug Actions build completed; workflow returned to manual-only mode)

**Non-guarantees**: every world event cannot be reported by every source instantly, Android sleeping phone wakeup is not supported by foreground WebSocket only, and 180-second app lifecycle does not control Android OS notification history, Cloudflare network logs, or external publisher copies.
