# Mirage Bank — Android app

Native Kotlin/Jetpack Compose client for the Mirage Bank demo backend (`../backend`), sideloaded rather than published to the Play Store. Since it's sideloaded, it updates itself: on launch it checks GitHub Releases for a newer signed APK, downloads it, and hands off to the standard Android package-installer for a one-tap install.

Full design rationale lives in the project plan; this file is the practical "how do I build and ship this" reference.

## Project structure

Single Gradle module (`app`), organized by feature package under `app/src/main/kotlin/com/mirage/bank/`:

- `core/network` — Retrofit `ApiService` (every backend endpoint), DTOs 1:1 with `backend/models.py`, auth/session-expiry interceptors.
- `core/session` — encrypted token storage, in-memory session state, local JWT expiry check.
- `core/update` — the self-update mechanism: GitHub Releases checker, DownloadManager wrapper, package-installer handoff.
- `core/navigation`, `core/theme`, `core/di` — nav route registry + host, Material3 theme, manual DI container.
- `feature/*` — one package per role/feature area (auth, home, personal, profile, youth_guardian, business, support, premium, admin, settings), each exposing a single `fun NavGraphBuilder.xGraph(...)` wired together in `core/navigation/MirageNavHost.kt`.

## Building for the first time

You'll need Android Studio (Hedgehog or newer) with an SDK Platform for API 34 installed — this repo has no bundled Gradle wrapper jar or `local.properties`, both of which Android Studio generates automatically on first import.

1. Open the `android/` folder as a project in Android Studio and let it sync (it will offer to create the Gradle wrapper — accept).
2. Generate the release signing key **once, ever** — do this before your very first build, not just before shipping:
   ```
   keytool -genkeypair -v -keystore mirage-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mirage-release
   ```
   Store `mirage-release.jks` **outside this repo** (password manager, or a GitHub Actions encrypted secret if you set up CI later). Losing it permanently breaks updates for every existing install — there's no recovery path, so back it up somewhere durable.
3. Copy `keystore.properties.example` to `keystore.properties` (already gitignored) and fill in the real path/passwords.
4. Build: `./gradlew assembleRelease`. **Always build release, even for your first local test install** — an update APK only installs over an existing one if signed with the same key, so a debug-signed first install would break the update chain for everything after it.
5. Sideload the resulting `app/build/outputs/apk/release/app-release.apk` to a device (adb install, or transfer + tap-to-install with "install unknown apps" enabled for whichever app you used to transfer it).
6. Bootstrap an admin test account so you can see the admin console: `cd ../backend && python3.13 make_admin.py your@email.com` (against the same Neo4j Aura instance the deployed backend uses), then log in with that account on-device.

## Cutting a new release (the update flow the app actually uses)

1. Bump `VERSION_NAME` in `version.properties` (`versionCode` is derived automatically — don't set it separately).
2. `./gradlew assembleRelease`.
3. Sanity-check the signature matches the previous release before publishing:
   ```
   apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
   ```
   Compare the SHA-256 cert fingerprint against your last release's. If it doesn't match, stop — something is wrong with the signing config and shipping this build will break updates for existing users.
4. Tag and push: `git tag android-v1.2.3 && git push origin android-v1.2.3` (note the `android-v` prefix — the in-app updater specifically looks for this prefix so it never confuses an Android release with any other tag in this repo).
5. Publish, attaching the signed APK with a stable filename:
   ```
   gh release create android-v1.2.3 app/build/outputs/apk/release/app-release.apk --title "Mirage Bank Android 1.2.3" --notes "..."
   ```
6. Installed apps pick this up on their next launch (throttled to roughly once per 6 hours) or immediately via Settings → "Check for updates".

## Verifying the update loop end-to-end (do this once before relying on it)

Install an older version manually, publish a newer `android-v` release, then open the app (or tap "Check for updates" in Settings) and confirm: update banner appears → download completes → tapping Install launches the system package installer → the app updates in place without needing "install unknown apps" re-prompting on subsequent updates.

## What this project deliberately does not do

No Play In-App Update API or App Bundle (this isn't going to the Play Store). No fabricated refresh-token flow — the backend has none, so the app re-prompts login when the 60-minute access token expires. No offline cache — every screen re-fetches on entry. Biometric unlock and a real app icon/splash are nice-to-haves, not implemented (see `app/src/main/res/drawable/ic_launcher_foreground.xml` — it's a placeholder wordmark).

## Known limitation of this handoff

This project was generated without access to an Android SDK or emulator, so the Kotlin/Gradle files here have been reviewed carefully (DTO field names, nav-graph wiring, and route/argument names were all cross-checked against `core/navigation/Routes.kt` and `core/network/ApiService.kt`) but have **not** been compiled. The first thing to do after opening this in Android Studio is a plain `./gradlew assembleDebug` — expect to fix a handful of minor issues (an unused import, a Compose API signature drift between BOM versions, etc.) rather than structural problems, since every feature package was built against the same fixed API contract and reuses the same handful of shared primitives (`ApiResult`, `GenericViewModelFactory`, `LocalAppContainer`, `Routes`).
