# TRENDNABIZ — Independent global event server (v0.2 foundation)

This subproject runs on a user-controlled Linux server with **Python 3.11+ standard library only**. No paid news API, Google Firebase, Redis, PostgreSQL or SaaS subscription is required by the software.

## What is operational in source code

- USGS worldwide earthquakes: official GeoJSON data.
- NASA EONET: curated natural-event metadata (wildfires, storms, etc.).
- NOAA/NWS: US government weather alerts.
- Optional RSS/Atom adapters, **disabled unless both written license approval flags are explicitly set**.
- SQLite data cache, immutable event IDs, source status, no duplicate alerts, per-source baseline (first sync does not blast historical news).
- Notification outbox, capped exponential retries and optional publishing to a **self-hosted ntfy** topic over HTTPS with bearer token.
- Public read-only JSON endpoint `/v1/events`, status `/healthz` and 2-minute SSE `/v1/stream` for foreground readers.

No external data feeds are connected to an actual live production server yet. Running the server is the deployment operator's responsibility.

## Quick start (Linux)

```bash
cd apps/trendnabiz
python3 -m server.runner --once
python3 -m server.runner
# locally: curl http://127.0.0.1:8808/v1/events
```

For public hosting, put the API behind a proper, maintained HTTPS reverse proxy and limit `TN_BIND` to loopback. Use systemd/container supervision, encryption, access logs without user identifiers, backups, monitoring and rate limits. An example configuration is in `.env.example`. This sample is **not automatically loaded**; provide values as process environment.

Use `TN_FEEDS_CONFIG` only with feeds that have documented authorization to redistribute both headlines and summaries. Do not upload third-party images, articles or videos merely because the metadata is available. Rights of publishers differ from rights of GDELT event metadata.

## Push delivery

Configure your own ntfy server with **auth-default-access: deny-all**, an ACL allowing read-only subscribing on the designated public event topic and a dedicated writer token. Do not expose the writer token in Android resources or public repository.

The backend publishes real data updates to self-hosted ntfy, but this code stage **does not yet register the Android application with UnifiedPush**; a suitable installed distributor and endpoint registration are still required for direct, app-branded background notifications. The 15-minute Android WorkManager fallback remains. Self-hosted ntfy can independently deliver notifications through its own client when installed/configured. No background delivery SLA has been measured.

Publishing a news item from this server does not establish that the incident is fully independently verified. USGS, NASA and NOAA are identified as their direct sources. A news publisher RSS feed must not be falsely labeled as official verification.

## Privacy and safety

No user names, location tracking, phone numbers, cookies or required account registration. HTTP API is read-only; secret credentials stay on the host. Third-party data is sanitized and length bounded before storage. Source links use HTTPS and URLs are not fetched from mobile users.

This app does **not** replace government emergency-warning channels or official evacuation instructions. Urgent messages must be explicitly classified and first-sync backlogs are not sent to sleeping users.

## Tests

```bash
cd apps/trendnabiz
python3 -m unittest discover -s server/tests -v
```

No CI/GitHub Actions build or release is triggered automatically by creation of these files.
