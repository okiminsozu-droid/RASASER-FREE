# TRENDNABIZ — RaSaSeR APPS & TECHNOLOGY

Independent notification-first Android application, source version **0.1.0**. Development work resides in `apps/trendnabiz` in RASASER-FREE. It does not modify other apps or the private RaSaSeR motor.

## Design and behavior
- Approved TRENDNABIZ brand: listener silhouette, blue digital globe, red pulse, blue TREND / red NABIZ, RaSaSeR signature.
- Light sky blue background, orange controls, black readable news text.
- Homepage: logo, bilingual slogan (TR/EN), publisher, clickable https://www.okiminsozu.com.
- Premium and Settings icons; five interface languages TR / EN / DE / ES / ZH; user changes language from a list.
- Notification-first: source visible on every headline; normal and critical notification channels, lock-screen visibility subject to user/device settings; source URL in article details.
- Notification actions: Translate, Listen, Share. These are gated behind verified Premium entitlement. The widget is Premium-gated too.
- Google Translate is an optional external handoff; Android TTS uses installed system speech engine, not a proprietary speech API.
- Basic news reading and notifications remain free. No fake subscriptions, promo-code bypass, simulated purchases or embedded secret.
- App stores source events on-device and supports an optional user-configured HTTPS backend; if unset, it reads USGS directly. No production backend is deployed yet.

## Technical boundaries
The initial feed is **real USGS data**, fetched over HTTPS. It must NOT be represented as all world news. A scheduled WorkManager check runs no more frequently than every 15 minutes and Android power-saving modes may delay it. Worldwide, instant, 99% reliable delivery requires a separate tested push infrastructure (self-hosted UnifiedPush/ntfy is an option), server deduplication, regional feed agreements, monitoring, and legal review.

Android lock-screen display, heads-up appearance, notification sounds, number of action buttons and visibility are ultimately controlled by Android/user/OEM settings. News copy inside system notifications cannot force black font color; inside the application black text is used.

Google Play digital subscriptions, when activated, must use applicable compliant billing / verified entitlement; the UI intentionally has no fake activation. No purchase API is wired in v0.1.0.

## Local build
With Java 17, Android SDK 35 and Gradle 8.9+ installed:
```sh
cd apps/trendnabiz
gradle :app:assembleDebug
```
No GitHub Actions workflow is run from this change. The public repository carries no signing key.

## Still needed before release
1. Verify build and run 5-language + notification tests on physical Android 8–16 devices.
2. Integrate self-hosted push server, event aggregation, source licenses, translation workflow.
3. Integrate verified Google Play Billing / entitlement and test premium actions + widget.
4. Perform image/right-to-use and legal review; prepare store privacy & content policies.
5. Publish signed AAB from a secure environment only after explicit approval.

## v0.2 server integration

`server/` now contains the independent Python/SQLite news aggregator, USGS/NASA/NOAA adapters, licensed RSS registry and optional self-hosted ntfy publishing. In Settings, enter the HTTPS reverse-proxy URL of your own deployed backend, or leave empty for direct USGS access. This is only an HTTP pull integration; UnifiedPush registration and production hosting have not been completed. Both direct and server feeds use local caching and a first-sync notification baseline.
