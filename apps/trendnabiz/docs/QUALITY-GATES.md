# TRENDNABIZ test and release gates

- [x] Separate public GitHub source folder; no modification to other apps, site, R2 maps or existing Cloudflare Workers
- [x] Distinct Cloudflare Worker and cron created; execution source has no news database writes
- [x] Native Android foreground WebSocket consumer and transient 180-second in-memory display coded
- [x] Existing Android persistent news cache and previous Python/SQLite server removed in this branch
- [ ] Android compile and real device verification
- [ ] WebSocket event reception + 3-minute expiry + 5 languages
- [ ] Long-running/locked/background notification delivery with genuine Android permissions and sustainable power behavior
- [ ] Press notification on lock-screen: source and actions work; no fabricated content
- [ ] Cloudflare Worker platform log/privacy verification, no news content in logs
- [ ] TLS health check from external device and active Cron events
- [ ] Strong source licensing and origin consistency for each additional publisher feed
- [ ] World-scale concurrency, rate limits, abuse handling, Cloudflare free/paid cap analysis
- [ ] Google Play billing, subscriber verification and compliant Privacy Policy
- [ ] Store/production approval (no Actions build initiated as part of this step)

**Non-guarantees**: every world event cannot be reported by every source instantly, Android sleeping phone wakeup is not supported by foreground WebSocket only, and 180-second app lifecycle does not control Android OS notification history, Cloudflare network logs, or external publisher copies.
