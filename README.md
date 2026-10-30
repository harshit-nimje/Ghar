<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="Ghar App Icon" width="112" height="112" />

# Ghar (घर)

**Give digital UPI money back its physical weight.**  
*A warm, illustrated Indian household finance & budgeting app built natively with Kotlin and Jetpack Compose.*

[![Build & Release APK](https://github.com/harshit-nimje/Ghar/actions/workflows/build-release-apk.yml/badge.svg)](https://github.com/harshit-nimje/Ghar/actions/workflows/build-release-apk.yml)
![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-4285F4?logo=jetpackcompose&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-Offline--First%20%2B%20Cloud%20Sync-D97736)

<br />

<img src="app/src/main/res/drawable-nodpi/img_house_entry_1792907129736.jpg" alt="Welcome to Ghar" width="680" />

</div>

---

## Why Ghar?

UPI made paying effortless—and made spending *weightless*. When ₹40 for chai, ₹280 for tiffin, and ₹450 for groceries vanish behind a four-digit PIN without a rustle of notes or clink of coins, monthly budgets quietly slip away before you feel them leave.

**Ghar** replaces sterile spreadsheets and guilt-heavy expense trackers with a living, illustrated Indian home. Instead of tabs and pie charts, you walk through five familiar rooms—watching your leather **Batua** (wallet) physically thin as you spend, dropping spare change into an earthen **Gullak**, and talking through everyday money choices with **Maa** and **Papa**.

---

## Step Inside the Five Rooms

Ghar is organized around five tactile spaces connected by a floating hallway dock, available in both **Warm Hinglish** (*Rasoi, Gullak, Chitthi*) and **Plain English** modes:

<table>
  <tr>
    <td width="33%" align="center">
      <img src="app/src/main/res/drawable-nodpi/img_kitchen_hearth_1792931327219.jpg" alt="Rasoi — Daily Budget" width="240" /><br />
      <b>1. Rasoi (Home)</b><br />
      <sub>Daily Budget & Maa's Hearth</sub>
    </td>
    <td width="33%" align="center">
      <img src="app/src/main/res/drawable-nodpi/img_gullak_sill_1792931330238.jpg" alt="Gullak — Savings Goals" width="240" /><br />
      <b>2. Gullak (Savings)</b><br />
      <sub>Bhai's Parallel Savings Jar</sub>
    </td>
    <td width="33%" align="center">
      <img src="app/src/main/res/drawable-nodpi/img_living_courtyard_1792931331737.jpg" alt="Courtyard — Monthly Analytics" width="240" /><br />
      <b>3. Courtyard (Insights)</b><br />
      <sub>Monthly Cashflow & Trends</sub>
    </td>
  </tr>
  <tr>
    <td width="33%" align="center">
      <img src="app/src/main/res/drawable-nodpi/img_study_desk_1792931360510.jpg" alt="Study — Budget & Growth" width="240" /><br />
      <b>4. Study (Plan)</b><br />
      <sub>Papa's Desk & Fixed Commitments</sub>
    </td>
    <td width="33%" align="center">
      <img src="app/src/main/res/drawable-nodpi/img_balcony_dusk_1792931332537.jpg" alt="Balcony — Preferences & Vault" width="240" /><br />
      <b>5. Balcony (Settings)</b><br />
      <sub>Language, SMS Capture & Cloud Sync</sub>
    </td>
    <td width="33%" align="center">
      <img src="app/src/main/res/drawable-nodpi/img_leather_batua_1792933032717.jpg" alt="Tactile Batua Wallet" width="240" /><br />
      <b>Physical Weight</b><br />
      <sub>Dynamic Batua & Coin Physics</sub>
    </td>
  </tr>
</table>

### What Happens in Each Room

- **Rasoi (Kitchen — Daily Budget):** See your remaining daily safe-to-spend balance inside a dynamic leather *Batua*, log expenses in one tap using customizable **Quick Chips** or the built-in **Hisaab Calculator Sheet**, read daily **Chitthi** reflections, and talk with **Maa** for grounded, empathetic feedback on day-to-day spending.
- **Gullak (Bhai's Room — Savings):** Build a parallel habit of paying yourself first. Drop coins into an earthen *Gullak* with tactile coin-drop animations and track milestone progress toward concrete goals (e.g., *Himachal Trip*, emergency cushion, or festival shopping).
- **Courtyard (Living Room — Insights):** Step back to view monthly spending velocity, category-wise breakdowns (*Chai & Snacks, Groceries, Food Delivery, Commute, Bills*), and recurring household patterns without clutter.
- **Study (Papa's Desk — Plan & Growth):** Manage monthly income/budget envelopes, track **Pakke Commitments** (rent, EMIs, subscriptions, utility bills), and consult **Papa** for structured, practical perspective on bigger purchases and long-term discipline.
- **Balcony (Evening Terrace — Preferences):** Switch seamlessly between **Hinglish** and **Plain English**, configure on-device **UPI/Bank SMS Auto-Capture**, manage daily reminders, export a clean **Household Summary**, or link your Google account for encrypted cloud backup.

---

## Core Features

- **Physical Money Metaphors:** Tactile cash and coin animations, dynamic *Batua* wallet states, and integer-paise precision (`amountPaise: Long`) across every calculation—zero floating-point rounding drift.
- **On-Device UPI & Bank SMS Auto-Capture:** Optional `SmsCaptureReceiver` and `SmsInboxScanner` parse Indian bank/UPI debit & credit alerts locally on your phone with automatic merchant extraction and duplicate suppression. Raw SMS content never leaves your device.
- **Maa & Papa Conversational Guidance (Hybrid Cloud + Offline AI):**
  - Context-aware household conversations powered by **Firebase AI Logic** with domain routing (`FamilyDomainRouter`).
  - Resilient offline & low-connectivity fallback via lazy on-device engines (`MaaMiniInferenceEngine` & `PapaOfflineAdvisor`) so guidance never blocks or fails when offline.
- **Instant Guest Vault + Seamless Cloud Upgrade:**
  - Start immediately with **zero sign-up** using the local `LocalHouseholdVault`.
  - Sign in anytime with Google (`androidx.credentials` Credential Manager) to automatically merge your guest ledger into user-scoped **Cloud Firestore** storage via `CloudSyncCoordinator`—with zero data loss.
- **Tuned for 90Hz / 120Hz Displays:**
  - Memory-bounded `GharImageCache` (`LruCache` with viewport-aware downsampling and deduplicated concurrent bitmap decoding).
  - Lazy AI initialization off the main thread, deferred idle resource prewarming, Compose compiler stability rules (`compose_compiler_config.conf`), and full R8 code + resource shrinking in release builds.

---

## Architecture & Tech Stack

Ghar follows an **offline-first MVVM + Repository** architecture with explicit constructor dependency injection (`RootAppContainer` / `AppContainer`) for fast cold startups and zero reflection overhead.

```text
app/src/main/java/com/example/
├── capture/          # On-device Bank/UPI SMS receiver, inbox scanner, parser & deduplicator
├── core/
│   ├── ai/           # FamilyDomainRouter, MaaMiniModelManager, MaaMiniInferenceEngine, PapaOfflineAdvisor
│   ├── audio/        # Tactile sound effects & subtle feedback cues
│   ├── engine/       # Budget pacing, safe-to-spend & financial calculation engines
│   ├── localization/ # Real-time Hinglish <-> Plain English vocabulary switching
│   ├── model/        # Immutable domain models (Transaction, SavingsGoal, Chitthi, Commitment)
│   └── perf/         # Deferred idle resource prewarming
├── data/
│   ├── local/        # LocalHouseholdVault (instant hot StateFlows + durable local snapshots)
│   ├── preferences/  # Jetpack DataStore user preferences
│   └── repository/   # Unified *Store interfaces with Local* and CloudSynced* implementations
└── ui/
    ├── kitchen/      # Rasoi screen & ViewModel
    ├── bhairoom/     # Gullak savings screen & ViewModel
    ├── livingroom/   # Courtyard analytics screen & ViewModel
    ├── study/        # Papa's Study budget & commitments screen & ViewModel
    ├── balcony/      # Settings, SMS permissions, export & account management
    └── common/       # Shared illustrations, Batua/Gullak graphics, Hisaab calculator, undo host
```

| Layer | Technologies |
| :--- | :--- |
| **Language & Async** | Kotlin `2.2`, Kotlin Coroutines & `StateFlow` |
| **UI Framework** | Jetpack Compose (Material 3), Edge-to-Edge `WindowInsets`, `HorizontalPager` |
| **Local Persistence** | `LocalHouseholdVault` + Jetpack `DataStore` Preferences |
| **Cloud & Auth** | Firebase Firestore, Firebase Auth, Jetpack Credential Manager (`GoogleId`), Firebase App Check |
| **AI Layer** | Firebase AI Logic + On-device offline fallback engines |
| **Build & Optimization** | AGP `9.1`, Compile/Target SDK `36`, Min SDK `24`, Compose Stability Config, R8 Shrinking |
| **Testing** | JUnit 4, Robolectric `4.16`, Kotlinx Coroutines Test, Firestore Security Rules Tests |

---

## Getting Started (Local Development)

### Prerequisites
- **Android Studio** Otter / Ladybug or newer (with **JDK 21** selected for Gradle)
- **Android SDK 36** (`compileSdk` 36.1, `minSdk` 24)
- **Gradle 9.3.1** (configured in `gradle/wrapper/gradle-wrapper.properties`)

### 1. Clone the Repository
```bash
git clone https://github.com/harshit-nimje/Ghar.git
cd Ghar
```

### 2. Environment & Firebase Setup
Copy the template environment file:
```bash
cp .env.example .env
```
- **Offline / Guest Mode:** Works out of the box—no external keys required to explore all five rooms, log transactions, track Gullak goals, or use offline household insights.
- **Cloud Sync & Cloud AI (Optional):** Place your Firebase project's `google-services.json` inside `app/google-services.json`, enable **Authentication (Google Sign-In)**, **Cloud Firestore**, and **Firebase AI Logic**, and register your signing key's **SHA-1** and **SHA-256** fingerprints in the Firebase Console.

### 3. Build & Run Tests
```bash
# Run JVM & Robolectric unit tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```

---

## Automated APK Builds & GitHub Releases (CI/CD)

This repository includes a manual GitHub Actions workflow at [`.github/workflows/build-release-apk.yml`](.github/workflows/build-release-apk.yml).

### How to Trigger a Build
1. Open the **Actions** tab in GitHub and select **Build & Release APK**.
2. Click **Run workflow** and choose:
   - **`build_variant`**: `release` (R8 minified & signed) or `debug`
   - **`version_tag`** *(optional)*: e.g., `v1.0.0` to automatically create a **GitHub Release** with the signed APK and its `.sha256` checksum attached. Leave blank to download the APK solely as a workflow artifact.

### Optional Repository Secrets for Persistent Release Signing
Under **Settings → Secrets and variables → Actions**, configure:

| Secret Name | Description |
| :--- | :--- |
| `RELEASE_KEYSTORE_BASE64` | Raw single-line Base64-encoded `.jks` / PKCS12 keystore (key alias **must** be `upload`) |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Password for the `upload` key alias |
| `GOOGLE_SERVICES_JSON` | *(Optional)* Contents of `app/google-services.json` if not committed to a fork |

> **Tip:** If signing secrets are omitted on a first test run, the workflow automatically generates an ephemeral fallback key so you still receive an immediately installable APK, and logs instructions in the workflow summary.

---

## Privacy & Security Principles

- **Zero Raw SMS Uploads:** Bank and UPI SMS parsing runs strictly on-device; raw message bodies are never transmitted to cloud servers.
- **Strict Per-User Isolation:** Cloud Firestore rules (`firestore.rules`) enforce zero-trust `request.auth.uid == userId` ownership boundaries across every household subcollection.
- **Integer-Paise Accounting:** Every transaction, budget limit, and savings goal is stored in integer paise (`Long`) to prevent floating-point inaccuracies.

---

<div align="center">
  <sub>Built with care for Indian households — where every rupee has a home.</sub>
</div>
