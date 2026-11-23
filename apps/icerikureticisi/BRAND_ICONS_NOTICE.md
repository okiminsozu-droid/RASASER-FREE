# RaSaSeR İçerik Üreticisi — Official Platform Symbols (v0.4.1)

The following seven brand glyphs are bundled as Android `res/drawable/brand_*.xml` vector resources so the user sees the actual logos **without an internet connection and even if the corresponding social app is not installed**: Instagram, Facebook, YouTube, TikTok, Pinterest, Bluesky and Telegram.

Sources: [Simple Icons](https://github.com/simple-icons/simple-icons), specifically `icons/{name}.svg` from `develop` retrieved 23 November 2026. The SVG shapes were converted into Android VectorDrawable XML paths, preserving the actual vector artwork. Original project distribution is dedicated to CC0-1.0, but individual trademark / logo rights remain with their respective owners. See [Simple Icons disclaimer](https://github.com/simple-icons/simple-icons/blob/develop/DISCLAIMER.md); the user and any distributor must comply with each platform's brand-identity requirements. Use does **not** imply affiliation or endorsement from these platforms.

The seven official logo images are strictly UI resources. None requires network retrieval. They do not imply the platform has authorized RaSaSeR to publish. Native direct connectors currently exist only for Telegram channel bots and Bluesky app-password sessions; all other five platforms remain user-selected local profile shortcuts until legitimate reviewed OAuth setup is available.

## Connection UI
- The creator page starts with a horizontal carousel of seven network icons + **Diğer** (custom social-network shortcut), all selectable; tapping a network opens its specific provider form.
- **Telegram:** channel username / numeric channel ID, account-password disabled, BotFather API key/token field masked. A verified bot must be administrator with post permission. Each bot/channel is stored separately in a device-only Keystore vault.
- **Bluesky:** user handle, masked app-specific password (never main login password), API-key disabled/not required. The app password is sent directly to Bluesky to issue short/refresh sessions; the original password is not persisted. Sessions are encrypted on the device.
- **Instagram, Facebook, YouTube, TikTok, Pinterest:** username/profile URL; account-password and API-key fields **disabled**. Only an unverified local shortcut can be stored. These platforms need approved official OAuth/API, and cannot be connected securely by collecting the regular account password.
- **Management:** a list of connected, verified Telegram/Bluesky accounts with status, one-account selection, network logo, per-connection re-verification and local deletion. Unlimited local entries, subject only to device memory/provider limits. Other manual shortcut entries remain below in the creator's existing list.
- **Privacy:** No developer-owned site is contacted and no account credentials are sent to RaSaSeR. Network calls for real publication or permissions go directly to the provider's official endpoints. Premium offline signature verification remains local. No private signing keys, refresh tokens or passwords are checked into this public repository.

## Tested scope and limitations
Android debug APK compilation and JUnit unit testing verify the local UI code; **only a successful provider API response after user-supplied authorization** verifies a live external channel. Integration tests using real users' social-network credentials are not run in CI. This release must not be described as automatic OAuth connectivity for all seven networks.

## Versions
Version 0.4.1 `versionCode 15`; separate debug package `com.rasaser.free.icerikureticisi.v041` installed side-by-side. Existing data from older `.v040` and other test packages are not automatically imported. The stable build, stored profiles, RaSaSeR website, other app modules and existing Cloudflare databases remain unchanged.
