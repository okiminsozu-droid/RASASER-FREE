# TRENDNABIZ Cloudflare ephemeral engine

Standalone Worker: **trendnabiz-engine**, no okiminsozu.com domain, no existing site modification. TLS is automatically served on the new workers.dev endpoint; the account's workers.dev hostname may still contain the account's historical subdomain string.

## Data lifecycle
- Cron trigger every minute checks USGS earthquakes, NOAA warnings (3-minute cadence), NASA EONET events (5-minute cadence).
- Factual source metadata is filtered to observations published in the last **180 seconds**. An event is transmitted to *currently attached* WebSocket clients.
- The singleton Durable Object retains only a transient `(event_id, expires_at)` deduplication Map for at most 180 seconds. It does **not** write event text, title, photos or URLs to SQLite, D1, R2, KV, Cache, Queues, logs or any news archive.
- The `/v1/live` WebSocket endpoint never replays or lists historic events. On Cloudflare isolate restart, deduplication IDs are lost. Events not received while the client is offline are deliberately not queued.
- Operational platform metadata, network-level traces and data on clients' devices are outside absolute erasure guarantees. Cloudflare Worker observability must be disabled and existing account-wide Logpush checked separately.
- `/healthz` contains server status only.
- **Important:** Android background notifications on a locked phone need an allowed foreground connection or a compatible push distributor. This Worker alone cannot wake an Android app that the OS stopped; 99% SLA is unverified.
- This is only the initial **3 source adapters**, not every news broadcaster globally. Full international coverage requires licensed publishers / allowed data sources, more regional adapters and measured resource budgets.
- NASA event geometry can be delayed beyond 3 minutes, and such events are intentionally skipped rather than storing or replaying them.

## No retained news
No database, uploaded media, publisher article mirror, feed archive or old-news viewer. No secrets in GitHub. Existing SQLite server is a separate prototype and is **not part of this Cloudflare engine**, because it would violate the new no-archive requirement.

## Build configuration
This project is stored as JavaScript module and a JSON design manifest; deployed over the Cloudflare Workers API independently, with one SQLite-class Durable Object solely for transient WebSocket coordination. **The storage API is never called.**

Open the `trendnabiz-engine` worker, verify observability is disabled, verify one-minute Cron Trigger is present, and inspect `/healthz` before relying on it.

## Scale
A single hub is suitable for early prototyping, not millions of active sockets. For a large deployment shard by region/subject and review Cloudflare cost, fanout and abuse prevention. Source-specific rate limits must be honored; do not spider websites without permission.

## Version 0.4 expansion (2026-11-20)
- Public probe at `/v1/probe` validates the websocket Durable Object binding; no old story endpoints.
- Adds GDACS globally reported disasters and European Commission official press releases. Because the source publishing timestamp must be within three minutes, slow feeds may not generate a notification.
- The source link always stays with the notice. No third-party article copies or images are stored. Worker still has no database, KV or R2 bindings.
- This is not a worldwide comprehensive/guaranteed-zero-delay news service; unrelated publisher content requires rights screening before adding.
