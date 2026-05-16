# ForzaBall

**Your ultimate fan app** — live scores, fixtures, league headlines, team discovery, and a social feed built around the clubs and competitions you care about.

[![Google Play](https://img.shields.io/badge/Google%20Play-ForzaBall-3DDC84?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.forzaball.pro)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![minSdk](https://img.shields.io/badge/minSdk-27-3DDC84?style=flat-square&logo=android)](app/build.gradle.kts)
[![targetSdk](https://img.shields.io/badge/targetSdk-36-3DDC84?style=flat-square&logo=android)](app/build.gradle.kts)

---

## Download

| Platform | Link |
|----------|------|
| **Google Play** | [ForzaBall App](https://play.google.com/store/apps/details?id=com.forzaball.pro) |

**Package:** `com.forzaball.pro`  
**Version name:** `1.0.1` · **Version code:** `2`  
*(See [`app/build.gradle.kts`](app/build.gradle.kts) for the current release constants.)*

---

## Author & links

| | |
|---|---|
| **Developer** | [Mahmoud Magdy](https://www.linkedin.com/in/mahmoudmagdy7) |
| **LinkedIn** | https://www.linkedin.com/in/mahmoudmagdy7 |
| **GitHub** | https://github.com/EngMahmoudMagdy |
| **Portfolio** | https://engmahmoudmagdy.github.io/ |
| **Source (public)** | https://github.com/EngMahmoudMagdy/forza-ball-public |

---

## Overview

ForzaBall is a native Android app for football fans who want **one place** for match day: personalized home content, live and upcoming fixtures, ESPN-powered news, team search with synced history, rich team profiles, sign-in, favorites, and a **social feed** with push notifications for posts, comments, and reactions.

The product is built with **Clean Architecture**, **Jetpack Compose**, and a **modular** codebase that is actively prepared for **Kotlin Multiplatform (KMP)** sharing between Android and future iOS targets.

---

## Features

### Match day & content
- **Personalized home** driven by favorite leagues and clubs (domestic + UEFA Champions League schedule merging where applicable).
- **Live scores & fixtures** from ESPN scoreboards and team schedules, with paging for long fixture lists.
- **League & team news** with in-app article reading (WebView) and dedicated news list flows.
- **Standings snapshots** for favorite teams via ESPN tables API (with KMP adapter path for shared logic).

### Discovery
- **Team & league search** with **recent search history** synced via Firestore when signed in.
- **Team profile screens** — crest, next match, news, and context for the selected club.

### Account & personalization
- **Onboarding** and multi-step **personalization** (leagues, teams, preferences).
- **Firebase Authentication** — email/password and **Google Sign-In**.
- **Profile**, edit favorites, theme preferences, and account deletion flow (hosted policy page + Cloud Functions).

### Social
- **Community feed** — create posts, comments, likes/dislikes.
- **Firestore-backed** real-time updates and **Firebase Cloud Messaging** for feed activity (Cloud Functions fan-out to FCM topics and per-user tokens).
- In-app notification center and high-priority Android notification channel for social events.

### Platform quality
- Branded **splash** and app icon, **edge-to-edge** Compose UI, light/dark theming.
- **Firebase Crashlytics**, **Analytics**, and **Performance Monitoring**.
- **Play Integrity** and **AndroidX Security Crypto** for hardened client practices.
- **CameraX** and **Media3 (ExoPlayer)** where media capture/playback is required.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  app (Compose UI, Navigation, ViewModels, Koin modules)    │
├─────────────────────────────────────────────────────────────┤
│  domain (models, repository contracts, business rules)       │
├─────────────────────────────────────────────────────────────┤
│  data (Retrofit/ESPN, Firestore, repository implementations) │
├─────────────────────────────────────────────────────────────┤
│  shared/* (KMP-ready: core-domain, core-auth, core-data,     │
│            shared-ui-compose, platform)                      │
└─────────────────────────────────────────────────────────────┘
```

| Layer | Responsibility |
|-------|----------------|
| **Presentation** | Compose screens, `ViewModel` / MVI-style state (`MviViewModel`), Navigation Compose, Koin |
| **Domain** | `AuthRepository`, `MatchRepository`, `NewsRepository`, `FeedRepository`, `PreferencesRepository`, pure Kotlin models |
| **Data** | ESPN HTTP (Retrofit + kotlinx.serialization), Firestore, DataStore preferences, paging sources |
| **Backend** | Firebase (Auth, Firestore, FCM, Cloud Functions in `functions/`) |

**Patterns:** Repository pattern, unidirectional UI state, coroutines + `Flow`, Paging 3 for news/fixtures lists, dependency injection with **Koin**.

**KMP rollout:** Shared modules compile on JVM/iOS simulator targets; Android keeps stable adapters until each feature is migrated ([`docs/kmp-rollout-checklist.md`](docs/kmp-rollout-checklist.md)).

---

## APIs & data sources

| Source | Usage |
|--------|--------|
| **[ESPN Site API](https://site.api.espn.com/apis/site/v2/sports/soccer/)** | Scoreboards, team schedules, news, team catalog |
| **[ESPN Soccer Tables API](https://site.api.espn.com/apis/v2/sports/soccer/)** | League standings |
| **Firebase Auth** | Email, Google Sign-In |
| **Cloud Firestore** | User profiles, feed posts/comments, notifications, search history sync, FCM token storage |
| **Firebase Cloud Messaging** | Push notifications (triggered from Cloud Functions on feed events) |
| **Firebase Cloud Functions** | Feed notification fan-out, account deletion, server-side Firestore batching |

> ESPN public endpoints are used for **soccer content** (fixtures, news, standings). Social data lives in **Firebase**.

---

## Tech stack

### Core
| Technology | Version (catalog) |
|------------|-------------------|
| Kotlin | 2.3.10 |
| Android Gradle Plugin | 9.0.0 |
| compileSdk / targetSdk | 36 |
| minSdk | 27 |
| JVM | 17 |

### UI
- Jetpack Compose (BOM `2026.02.01`), Material 3, Material adaptive navigation suite
- Navigation Compose · Coil · Accompanist (swipe refresh, system UI)
- CameraX · Media3 ExoPlayer

### Architecture & async
- Clean Architecture modules (`:app`, `:domain`, `:data`, `:shared:*`)
- Coroutines · StateFlow · Paging 3 Compose
- Koin 4.1.1

### Networking
- Retrofit 3 + OkHttp 5 + kotlinx.serialization
- Ktor client (KMP shared modules)
- Chucker (debug), Timber logging

### Persistence & background
- DataStore Preferences
- Room (catalog; used where local cache is enabled)
- WorkManager

### Firebase & Google
- Firebase BOM 34.11.0 — Auth, Firestore, FCM, Crashlytics, Analytics, Performance
- Google Play Services Auth
- Play Integrity API

---

## AI-assisted development

ForzaBall was built with a modern **AI-augmented workflow** alongside traditional engineering practices:

| Tool | Role |
|------|------|
| **[Cursor](https://cursor.com/) agents** | Feature scaffolding, refactors, test generation, DI/navigation wiring, and rapid iteration across modules |
| **LLM pair programming** | API mapping (ESPN DTOs → domain models), Firestore rules alignment, paging/Compose state fixes |
| **[Google Stitch](https://stitch.withgoogle.com/)** | UI exploration, screen layout direction, and design-to-implementation handoff for Compose screens |
| **Human review** | Architecture decisions, security (Firestore rules, Play Integrity), UX polish, and store release |

Design intent from **Stitch** was translated into **Material 3 Compose** components and the app’s theme system—not pixel-copied assets—so the app stays maintainable and accessible.

---

## Project structure

```
ForzaBall/
├── app/                    # Compose UI, MainActivity, feature packages
├── domain/                 # Repository interfaces & domain models
├── data/                   # ESPN, Firestore, repository implementations
├── shared/
│   ├── core-domain/
│   ├── core-auth/
│   ├── core-data/
│   ├── platform/
│   └── shared-ui-compose/  # KMP UI primitives (rollout in progress)
├── functions/              # Firebase Cloud Functions (Node.js)
├── hosting/                # Static pages (e.g. account deletion)
└── docs/                   # KMP rollout checklist
```

---

## Getting started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 36

### Clone & run
```bash
git clone https://github.com/EngMahmoudMagdy/forza-ball-public.git
cd forza-ball-public
```

1. Add `google-services.json` from your Firebase project to `app/`.
2. For release builds, create `store.properties` at the project root (see [`store.properties`](store.properties) pattern; file is gitignored).
3. Sync Gradle and run:

```bash
./gradlew :app:installDebug
```

### Useful tasks
```bash
./gradlew :app:compileDebugKotlin
./gradlew :shared:core-domain:compileKotlinJvm
```

---

## Testing

- Unit tests for ESPN DTO mapping and domain preference logic (`data/src/test`, `domain/src/test`)
- Compose UI test dependencies configured for instrumented/UI tests

```bash
./gradlew testDebugUnitTest
```

---

## Privacy & compliance

- [Google Play listing](https://play.google.com/store/apps/details?id=com.forzaball.pro) — data safety declarations (personal info, photos/videos, encryption in transit, deletion requests)
- Account deletion hosted page under `hosting/account-delete/`

---

## License

All rights reserved © Mahmoud Magdy.  
Source is published for portfolio and technical review; commercial use of the codebase requires permission.

---

## Contact

- **LinkedIn:** [mahmoudmagdy7](https://www.linkedin.com/in/mahmoudmagdy7)
- **GitHub:** [@EngMahmoudMagdy](https://github.com/EngMahmoudMagdy)
- **Portfolio:** [engmahmoudmagdy.github.io](https://engmahmoudmagdy.github.io/)
- **Play Store:** [ForzaBall App](https://play.google.com/store/apps/details?id=com.forzaball.pro)
