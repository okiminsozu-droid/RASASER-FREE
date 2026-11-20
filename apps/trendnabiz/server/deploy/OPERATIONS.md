# Own-server deployment checklist

This repository contains **only files and examples**; no server, DNS, notification gateway or TLS certificate has been provisioned.

1. Choose an independently administered Linux server. Install Python 3.11+, a maintained reverse proxy, a domain and valid TLS certificate, with backups.
2. Copy only the `server/` package to `/opt/trendnabiz/server/`, keeping parent `/opt/trendnabiz` as working directory.
3. Create the non-login OS user `trendnabiz` and writable `/var/lib/trendnabiz` storage. Configure `TN_DATABASE=/var/lib/trendnabiz/events.sqlite3`.
4. Edit `/etc/trendnabiz/server.env` with `TN_BIND=127.0.0.1`, `TN_PORT=8808`; optionally set a user-owned ntfy host and publisher credential. This file must never go into public GitHub.
5. Configure a valid HTTPS reverse proxy and install the service under systemd, check with `systemctl status` and `curl https://YOUR_DOMAIN/healthz`.
6. Set the HTTPS base URL in TRENDNABIZ Android Settings. Initially run `python3 -m server.runner --once` to establish source baselines.
7. For ntfy create ACL-deny-by-default, one read-only public event topic, one writer with a token; verify the token cannot be read by clients and writing to any other topic fails.
8. Before any public rollout: record rights for enabled RSS publishers, add source monitoring, SSL renewal checks, rate limiting, backup restore tests, audit trails, safe source-disable switch.
9. Do an instrumented Android background/locked-screen notification test across manufacturers before claiming a percent delivery success.

## Important independent push limitation
The code publishes to the ntfy service (when configured), **not directly into the TRENDNABIZ Android process while it is asleep**. Direct app-branded push requires implementing UnifiedPush registration/endpoint handling or another compliant distribution path, plus a compatible distributor on devices. The current Android fallback polls every 15 minutes at best and can be delayed by battery optimization. Production push latency is not yet measurable.
