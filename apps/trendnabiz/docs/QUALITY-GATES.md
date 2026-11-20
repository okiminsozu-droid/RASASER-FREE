# TRENDNABIZ quality gates (v0.1.0)

## Isolation
- All new files stay under apps/trendnabiz/.
- No change to RaSaSeR PRIVATE motors, other Android apps, Cloudflare data or okiminsozu.com.
- No secrets, signing keys, billing credentials or proprietary news copies in PUBLIC GitHub.

## Required real-device tests before release
1. Android 8–16, five language changes, rotation/resize, offline state, website click.
2. Android 13+ prompt for POST_NOTIFICATIONS at user's command, disabled/denied state.
3. Android 8+ separate critical and normal notification channels, device locked/unlocked, content and sound.
4. Lock-screen source, branded large icon where supported, open/Translate/Listen/Share actions and their premium gate.
5. Periodic 15-minute USGS refresh, deduplication, no alert flood on first sync, offline cached news and after reboot.
6. Notification latency percentiles from server event ingest to Android displayed, measured on major OEMs and battery modes; do NOT assert 99% before instrumented measurements.
7. User privacy, Google Translate external handoff disclaimer and share source attribution.
8. Premium entitlement verified on server/Google Play before unlock, widget lock state and revocations.
9. All global news sources licensed and verified before adding adapters.

## Explicit v0.1.0 limitations
- Only USGS earthquakes are loaded; worldwide all-topic news ingestion is not online.
- Android WorkManager 15-minute minimum prevents guaranteed instantaneous delivery.
- The ntfy/UnifiedPush self-hosted push server is not deployed or connected.
- Google Play Billing / Premium purchase verification is not connected.
- Test button sends a labeled local test notification, not a fabricated news event.
- No CI build or APK was run by this commit. Follow the project's explicit review-before-Actions policy.
- The approved artwork is embedded as lightweight WEBP build assets; keep the original high-resolution image backed up outside the public repo for final store assets.
