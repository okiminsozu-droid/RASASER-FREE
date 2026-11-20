# TRENDNABIZ — RaSaSeR independent live notification app

Branch: feature/trendnabiz-notification-first-v1
Android: com.rasaser.free.trendnabiz
Cloudflare: trendnabiz-engine (separate Worker on the connected Cloudflare account)
Source code release: 0.2.0, NO APK or AAB built/tested/released.

## Approved design
Blue/red TRENDNABIZ with listener silhouette, digital globe and red pulse; pale blue homepage, orange controls and black legible event text. Turkish and English slogans, RaSaSeR APPS & TECHNOLOGY, website **www.okiminsozu.com** in brand footer ONLY — this existing website is NOT used as the news backend or modified. Settings, Premium indicator, five UI languages TR/EN/DE/ES/ZH.

## New architecture (no news archive)
- Cloudflare Worker `cloudflare/TRENDNABIZ/worker.mjs`, independently deployed as `trendnabiz-engine`.
- Cron scans 1-minute cycles: USGS earthquakes (each minute), NOAA alerts (every third minute), NASA EONET (every fifth minute).
- Only event records with a *source time* from the previous 180 seconds are broadcast to open WebSocket clients. No old news backfill.
- Worker Durable Object keeps at most transient **event identifiers** for deduplication. No article contents, texts, images, or event archive are written to DO storage, KV, D1, R2, Cache or Queues.
- Cloudflare Worker observability disabled. Cloudflare may still retain network/platform operational metadata outside our application control. Absolute zero retention cannot be guaranteed.
- Android uses a websocket session **while the app is visible** to listen for new events, produces a branded native Android notification with source, and clears the in-memory live list after three minutes. The earlier Android SharedPreferences news cache and scheduled WorkManager-based history fetching are removed.
- Android OS notification shade/history is outside app retention guarantees; tapping a notification may pass its event text through system PendingIntent extras until Android discards it.
- Premium actions (Translate / TTS Listen / Share / home-screen widget) remain locked on production builds without verified billing; debug-only preview is not an entitlement.
- Five interface languages are present, but arbitrary world headlines from external sources are **not automatically translated**. Only known, structured USGS event text can be rendered in those five languages.
- The worker currently has **three official-source adapters**, not worldwide comprehensive news coverage. Publisher licenses, other country feeds and category mappings remain to be added.

## Status, important limitations
**Worker deployed, cron enabled.** No installed Android APK, real-device tests or production push have been confirmed. The Worker broadcasts to currently connected clients but **cannot wake a sleeping Android app**. The Android code disconnects on background: locked-phone / app-closed instant notifications are NOT implemented or verified. A policy-compliant, user-consented background delivery method must be researched/tested before release. Do not claim 99% delivery.

No Cloudflare credentials, sign keys, publisher full-text articles or user data stored in public GitHub. The old Python/SQLite experimental server has been removed from this branch per no-news-archive requirement.

## Testing and next gates
1. Review compile/release configuration, perform Android debug build after feature approval.
2. Connect on test device and inspect live events received from Cloudflare.
3. Build compliant background push/connection strategy; instrument lock-screen delivery by manufacturer and OS.
4. Add vetted global publisher sources and rights register; verify no non-permitted text is redistributed.
5. Verify all news content disappears after 180 seconds from app state, and Worker logs/metrics remain configured without event content.
6. Integrate verified Play Billing or other compliant entitlement before Premium release.
