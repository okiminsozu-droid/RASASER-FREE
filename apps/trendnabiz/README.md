# TRENDNABIZ — RaSaSeR independent live notification app

Branch: feature/trendnabiz-notification-first-v1
Android: com.rasaser.free.trendnabiz
Cloudflare: trendnabiz-engine (separate Worker on the connected Cloudflare account)
Test APK version: 0.3.0. GitHub isolated debug build and JVM unit tests succeeded (run 44795358502). No signed release AAB or Play Store deployment.

## Approved design
Blue/red TRENDNABIZ with listener silhouette, digital globe and red pulse; pale blue homepage, orange controls and black legible event text. Turkish and English slogans, RaSaSeR APPS & TECHNOLOGY, website **www.okiminsozu.com** in brand footer ONLY — this existing website is NOT used as the news backend or modified. Settings, Premium indicator, five UI languages TR/EN/DE/ES/ZH.

## New architecture (no news archive)
- Cloudflare Worker `cloudflare/TRENDNABIZ/worker.mjs`, independently deployed as `trendnabiz-engine`.
- Cron scans 1-minute cycles: USGS earthquakes (each minute), NOAA alerts (every third minute), NASA EONET and European Commission RSS (every fifth minute), GDACS RSS (every sixth minute). A source-reported timestamp older than 180 seconds is intentionally skipped (no backfill); this can miss slower publisher reports.
- Only event records with a *source time* from the previous 180 seconds are broadcast to open WebSocket clients. No old news backfill.
- Worker Durable Object keeps at most transient **event identifiers** for deduplication. No article contents, texts, images, or event archive are written to DO storage, KV, D1, R2, Cache or Queues.
- Cloudflare Worker observability disabled. Cloudflare may still retain network/platform operational metadata outside our application control. Absolute zero retention cannot be guaranteed.
- Android uses a WebSocket session while visible; in debug APK a user may explicitly enable an experimental foreground live-monitoring service with a persistent Stop control to test locked/background delivery. The service is disabled in release builds pending separate permission policy review. Alerts include source, a three-minute Android system notification timeout and an in-memory transient display. The earlier Android SharedPreferences news cache and scheduled WorkManager-based history fetching are removed.
- Android OS notification shade/history is outside app retention guarantees; tapping a notification may pass its event text through system PendingIntent extras until Android discards it.
- Premium actions (Translate / TTS Listen / Share / home-screen widget) remain locked on production builds without verified billing; debug-only preview is not an entitlement.
- Five interface languages are present, but arbitrary world headlines from external sources are **not automatically translated**. Original explanatory summaries for the five enabled official source types are provided in five languages; third-party titles are still in their source language.
- The worker currently has **five official-source adapters**, not worldwide comprehensive news coverage. Publisher licenses, other country feeds and category mappings remain to be added.

## Status, important limitations
**Worker deployed, cron enabled; external HTTPS health, hub readiness and internal WebSocket handshake tested successfully.** The Android debug build succeeded; physical-device reception tests and always-on production push have NOT been verified. The Worker broadcasts to clients that remain connected, and cannot by itself wake a sleeping/force-stopped Android app. A user-initiated debug foreground service is included, but may be stopped or network-throttled by Android or OEM power restrictions. A policy-compliant, user-consented background delivery method must be researched/tested before release. Do not claim 99% delivery.

No Cloudflare credentials, sign keys, publisher full-text articles or user data stored in public GitHub. The old Python/SQLite experimental server has been removed from this branch per no-news-archive requirement.

## Testing and next gates
1. Android debug APK built; device-level permission, service, lock-screen and network-offline tests still required.
2. Connect on test device and inspect live events received from Cloudflare.
3. Build compliant background push/connection strategy; instrument lock-screen delivery by manufacturer and OS.
4. Add vetted global publisher sources and rights register; verify no non-permitted text is redistributed.
5. Verify all news content disappears after 180 seconds from app state, and Worker logs/metrics remain configured without event content.
6. Integrate verified Play Billing or other compliant entitlement before Premium release.

## v0.3.0 Repair test build
- Cloudflare live websocket hub verified through public handshake diagnostic. Five official-source adapters: USGS, NASA EONET, NOAA/NWS, GDACS, European Commission; not every worldwide news publisher.
- Android error diagnostics are visible. Story content and identifiers are not saved; system notification timeout after 180 seconds. The OS-level notification history remains outside app control.
- Debug-only user opt-in foreground service can keep a WebSocket connection running while the UI is in the background, with a persistent stop button. Android OEM power management can still suspend it. Google Play review is required before considering any release version with foreground special-use service.
- Production release manifest intentionally does not declare that debug-only foreground service. No 99% guarantee or FCM push is connected.
- Language-specific original summary templates provided for the five verified source families. Third-party English headlines are not silently machine-translated.
