# GHAR — Android App Design Specification

**Date:** 2026-10-25
**Status:** Approved for implementation planning
**Scope:** Core MVP Slice (Onboarding → House Entry → Kitchen with one insight → Bhai's Gullak)

---

## 1. Project Overview

### 1.1 Vision
A native Android budgeting app that makes UPI money feel physical again, then shows the user a parallel life where they saved it — without ever making them feel small. Two characters: **Maa** (motherly coach) and **Bhai** (elder-brother parallel wallet). Navigation via a **house metaphor** — each screen is a room.

### 1.2 Core MVP Slice (from Brief §11)
1. **Onboarding** — MCQ flow ending at the front door
2. **House entry animation** — spatial transition into the home
3. **Kitchen** — Maa's domain with one working pattern-based insight (repeated small leak)
4. **Bhai's Room** — Gullak (parallel savings wallet) with named destination

---

## 2. Architecture Approach

**Approach C: Single-module pragmatic** (approved)

- Single Android module, single Activity
- Pure Kotlin engine in `core/` package (JVM-testable)
- Compose Navigation, MVVM with Hilt DI
- Room for ledger, DataStore for preferences
- NotificationListenerService for UPI capture
- Material 3 Expressive, warm homely palette

---

## 3. Package Structure

```
com.ghar.app
├── core/                    # Pure Kotlin engine (testable on JVM)
│   ├── engine/
│   │   ├── SafeToSpendCalculator.kt
│   │   ├── PatternDetector.kt
│   │   ├── InsightEngine.kt
│   │   └── InsightTemplates.kt
│   ├── model/
│   │   ├── Transaction.kt
│   │   ├── Category.kt
│   │   ├── BudgetProfile.kt
│   │   ├── SavingsGoal.kt
│   │   ├── Insight.kt
│   │   ├── GuiltSensitivity.kt
│   │   ├── HabitType.kt
│   │   ├── PainDial.kt
│   │   ├── Direction.kt
│   │   ├── CaptureSource.kt
│   │   ├── InsightTone.kt
│   │   └── InstrumentType.kt
│   └── util/
│       ├── DateUtils.kt
│       └── CurrencyFormatter.kt
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── TransactionDao.kt
│   │   │   ├── BudgetDao.kt
│   │   │   ├── GoalDao.kt
│   │   │   └── InsightDao.kt
│   │   ├── entity/
│   │   │   ├── TransactionEntity.kt
│   │   │   ├── BudgetProfileEntity.kt
│   │   │   ├── SavingsGoalEntity.kt
│   │   │   └── InsightEntity.kt
│   │   ├── repository/
│   │   │   ├── TransactionRepository.kt
│   │   │   ├── BudgetRepository.kt
│   │   │   ├── GoalRepository.kt
│   │   │   └── InsightRepository.kt
│   │   └── GharDatabase.kt
│   └── preferences/
│       └── PreferencesRepository.kt (DataStore)
├── capture/
│   ├── NotificationCaptureService.kt (NotificationListenerService)
│   ├── SmsCaptureReceiver.kt
│   ├── parser/
│   │   ├── UpiNotificationParser.kt
│   │   ├── SmsParser.kt
│   │   └── MerchantLookupTable.kt
│   └── dedupe/
│       └── TransactionDeduplicator.kt
├── ui/
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Routes.kt
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt
│   │   ├── OnboardingViewModel.kt
│   │   ├── steps/
│   │   │   ├── AgeBandStep.kt
│   │   │   ├── LifeStageStep.kt
│   │   │   ├── IncomeStep.kt
│   │   │   ├── FixedVsFlexibleStep.kt
│   │   │   ├── HabitTypeStep.kt
│   │   │   ├── GuiltSensitivityStep.kt
│   │   │   ├── SavingsGoalStep.kt
│   │   │   └── PainDialStep.kt
│   │   └── OnboardingProgress.kt
│   ├── house/
│   │   ├── HouseScreen.kt
│   │   ├── HouseViewModel.kt
│   │   ├── EntryAnimation.kt
│   │   └── HallwayAffordance.kt
│   ├── kitchen/
│   │   ├── KitchenScreen.kt
│   │   ├── KitchenViewModel.kt
│   │   ├── SafeToSpendDisplay.kt
│   │   ├── MaaInsightCard.kt
│   │   ├── TransactionList.kt
│   │   └── QuickAddFab.kt
│   ├── bhairoom/
│   │   ├── BhaiRoomScreen.kt
│   │   ├── BhaiRoomViewModel.kt
│   │   ├── GullakView.kt
│   │   ├── GoalProgress.kt
│   │   └── ReinvestIllustration.kt
│   ├── study/
│   │   └── (placeholder for expansion)
│   ├── livingroom/
│   │   └── (placeholder for expansion)
│   ├── balcony/
│   │   └── (placeholder for expansion)
│   ├── common/
│   │   ├── theme/
│   │   │   ├── Theme.kt
│   │   │   ├── Color.kt
│   │   │   ├── Typography.kt
│   │   │   └── Shapes.kt
│   │   ├── components/
│   │   │   ├── GharButton.kt
│   │   │   ├── GharCard.kt
│   │   │   ├── GharTextField.kt
│   │   │   └── Avatar.kt
│   │   ├── animation/
│   │   │   ├── PhysicalCashAnimation.kt
│   │   │   ├── SpringSpecs.kt
│   │   │   └── WalletThinning.kt
│   │   └── util/
│   │       └── NavigationExtensions.kt
│   └── viewmodel/
│       ├── SharedViewModels.kt (Hilt-provided)
│       └── ViewModelFactory.kt
└── di/
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    ├── CaptureModule.kt
    ├── EngineModule.kt
    └── ViewModelModule.kt
```

---

## 4. Core Engine (Pure Kotlin — Zero Android Dependencies)

### 4.1 SafeToSpendCalculator

```kotlin
data class BudgetProfile(
    val monthlyIncome: Long,        // paise
    val fixedCommitments: Long,     // paise
    val savingsTarget: Long,        // paise
    val monthStartDay: Int = 1      // 1-28
)

data class SafeToSpendResult(
    val dailyAllowance: Long,       // paise per day
    val daysRemaining: Int,
    val totalRemaining: Long,       // paise
    val paceRatio: Double,          // spentSoFar / expectedSoFar
    val projectedMonthEnd: Long     // paise projection
)

object SafeToSpendCalculator {
    fun calculate(profile: BudgetProfile, spentSoFar: Long, currentDate: LocalDate = LocalDate.now()): SafeToSpendResult
}
```

**Formula:** `(income − fixed − savingsTarget − spentSoFar) / daysRemaining`

### 4.2 PatternDetector

```kotlin
data class PatternAnalysis(
    val frequencyByMerchant: Map<String, Int>,      // 7-day window
    val frequencyByCategory: Map<Category, Int>,    // 7-day window
    val trendVsPreviousPeriod: Map<Category, Trend>, // 30-day vs prior 30-day
    val anomalies: List<Anomaly>,                   // large, unusual time, new merchant
    val guiltSafetySignals: List<GuiltSignal>       // medical/necessary expense detected
)

enum class Trend { UP, DOWN, STABLE }
data class Anomaly(val type: AnomalyType, val transaction: Transaction, val details: String)
data class GuiltSignal(val type: GuiltSignalType, val transaction: Transaction)

object PatternDetector {
    fun analyze(transactions: List<Transaction>, currentDate: LocalDate = LocalDate.now()): PatternAnalysis
}
```

- Rolling 7-day and 30-day windows
- Frequency counts per merchant/category
- Trend vs previous period (30-day comparison)
- Anomaly detection: unusually large (>2σ), unusual time (22:00-06:00), new merchant (≥3× in 14 days)
- Guilt-safety signals: medical/necessary expense keywords, large single transactions

### 4.3 InsightEngine

```kotlin
data class Insight(
    val id: String,
    val templateId: String,
    val renderedText: String,
    val tone: InsightTone,
    val priority: InsightPriority,
    val triggeredAt: Long
)

enum class InsightPriority { SAFETY, SAVINGS_OPPORTUNITY, PRAISE, STREAK }
enum class InsightTone { WARM, GENTLE, INFORMATIVE, PRACTICAL, REASSURING, QUESTION, OFFER, PRAISE, WELCOME, SILENCE }

object InsightEngine {
    fun generateInsights(
        profile: BudgetProfile,
        transactions: List<Transaction>,
        analysis: PatternAnalysis,
        guiltSensitivity: GuiltSensitivity,
        cooldownState: CooldownState
    ): List<Insight>
}
```

**Constraints (from brief §5.3):**
- Max 1 proactive insight per day
- Max 3 per week
- Prioritization: SAFETY > SAVINGS_OPPORTUNITY > PRAISE > STREAK
- Cooldown tracking per template (template-specific + global daily/weekly)

### 4.4 InsightTemplates

20 templates from brief §5, each with:
- **Trigger condition** (predicate on PatternAnalysis + BudgetProfile)
- **Text template** with slots: `{thing}`, `{n}`, `{amount}`, `{category}`, `{days}`, `{merchant}`, `{goal}`, `{alternative}`
- **Tone** (InsightTone)
- **Cooldown** (days before re-fire)

**GuiltSensitivity adjustments:**
- LOW: tighter thresholds, firmer tone, more frequent insights
- MEDIUM: balanced (default)
- HIGH: looser thresholds, gentler tone, fewer insights, longer cooldowns

---

## 5. Data Layer

### 5.1 Room Entities

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Long,                    // paise
    val direction: Direction,            // DEBIT / CREDIT
    val merchant: String?,
    val category: Category,
    val timestamp: Long,                 // epoch millis
    val source: CaptureSource,           // NOTIFICATION / SMS / MANUAL
    val rawText: String?,
    val isRecurring: Boolean = false
)

@Entity(tableName = "budget_profile")
data class BudgetProfileEntity(
    @PrimaryKey val id: Int = 1,
    val monthlyIncome: Long,
    val fixedCommitments: Long,
    val savingsTarget: Long,
    val monthStartDay: Int,
    val guiltSensitivity: GuiltSensitivity,  // LOW / MEDIUM / HIGH
    val habitType: HabitType,
    val painDial: PainDial
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val targetDate: Long?,
    val instrumentType: InstrumentType  // SIP / RD / PPF / NONE
)

@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val renderedText: String,
    val tone: InsightTone,
    val triggeredAt: Long,
    val isRead: Boolean = false
)
```

### 5.2 DataStore Preferences

| Key | Type | Purpose |
|-----|------|---------|
| `onboarding_completed` | Boolean | Gate entry animation |
| `guilt_sensitivity` | String (enum) | Maa calibration |
| `pain_dial` | String (enum) | Animation intensity |
| `sound_enabled` | Boolean | Cash sound opt-in |
| `haptics_enabled` | Boolean | Haptic feedback |
| `notification_permission_granted` | Boolean | Track permission state |
| `last_insight_date` | Long | Daily cooldown |
| `weekly_insight_count` | Int | Weekly limit (reset Monday) |
| `last_weekly_reset` | Long | Weekly counter reset timestamp |

### 5.3 Repositories

- **TransactionRepository**: CRUD + `getByDateRange()`, `getByCategory()`, `getByMerchant()`, `getSpentThisMonth()`, `getRecent(limit)`
- **BudgetRepository**: `getProfile()`, `updateProfile()`, `observeProfile()` (Flow)
- **GoalRepository**: `getGoal()`, `updateGoal()`, `incrementCurrent(amount)`, `observeGoal()` (Flow)
- **InsightRepository**: `getUnread()`, `markRead(id)`, `insert(insight)`, `getCooldownState()`

---

## 6. Capture Layer (NotificationListenerService + SMS)

### 6.1 NotificationCaptureService

```kotlin
class NotificationCaptureService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Filter: GPay, PhonePe, Paytm, bank apps
        // Parse via UpiNotificationParser
        // Dedupe via TransactionDeduplicator
        // Insert via TransactionRepository
        // Trigger PhysicalCashAnimation via event bus
    }
}
```

**Target apps:** `com.google.android.apps.nbu.paisa.user` (GPay), `com.phonepe.app`, `net.one97.paytm`, major bank packages

### 6.2 Parsers

- **UpiNotificationParser**: Regex extraction of amount, merchant, timestamp, direction from notification text
- **SmsParser**: Sender ID allowlist + regex for bank/UPI SMS formats
- **MerchantLookupTable**: Known merchant → category mapping (covers ~90% per brief)

### 6.3 Deduplication

Match on `amount + timestamp ± 2 minutes + direction` across notification + SMS sources.

---

## 7. UI/UX Design

### 7.1 Navigation Model

- **MainActivity** → Compose NavHost
- **Routes**: `onboarding/{step}`, `house`, `kitchen`, `bhairoom`, `study`, `livingroom`, `balcony`
- **Hallway**: Persistent bottom sheet (swipe up) or floating action button for quick room switching

### 7.2 Entry Animation (Front Door → House)

```kotlin
// HouseScreen.kt
AnimatedContent(
    transitionSpec = {
        fadeIn(spring(stiffness = SpringStiffnessMedium, dampingRatio = SpringDampingRatioMedium)) +
        slideIntoContainer(slideX = { -it.width }, spring(...))
    }
) { targetState ->
    when (targetState) {
        HouseState.Entry -> EntryAnimation()
        HouseState.Kitchen -> KitchenScreen()
        // ...
    }
}
```

- First launch only (DataStore `onboarding_completed` = false)
- Door opens → interior reveals → settle in Kitchen
- Subsequent launches: direct to Kitchen

### 7.3 Room Specifications

#### Kitchen (Maa's Domain)
- **SafeToSpendDisplay**: Oversized display type (Material 3 Expressive `displayLarge`), animated counter
- **MaaInsightCard**: Single card, warm terracotta background, Maa avatar, insight text, dismiss action
- **TransactionList**: LazyColumn, grouped by date, merchant avatar + category chip
- **QuickAddFab**: Floating action button → manual entry bottom sheet

#### BhaiRoom (Gullak)
- **GullakView**: Animated clay piggy bank (Lottie or custom Canvas), subtle idle animation (breathing)
- **Current Amount**: Large type, "We have ₹X" framing
- **GoalProgress**: Circular progress toward named goal
- **ReinvestIllustration**: "If this pace kept up, at typical SIP rate (12%), this grows to ~₹Y in Z years" — illustrative disclaimer visible

#### Study / LivingRoom / Balcony
- Placeholder screens for MVP (navigation works, UI stubbed)

### 7.4 Physical Cash Animation (Signature Interaction)

```kotlin
@Composable
fun PhysicalCashAnimation(
    amount: Long,
    targetRect: Rect,  // merchant card position
    onComplete: () -> Unit
) {
    // Calculate bill count: min(max(amount / 100, 1), 20)  // 1-20 bills
    // Animate from bottom center → targetRect
    // Spring physics with overshoot
    // Haptic: VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
    // Sound: MediaPlayer.play(paper_rustle) if enabled
}
```

**Scaling:**
- ₹40 → 1 note
- ₹400 → 4 notes
- ₹4,000 → fat stack (15-20 notes, layered)
- Visual weight = `log10(amount) * baseWeight`

**Wallet Thinning:** Safe-to-spend display background width animates proportionally to `totalRemaining / (income - fixed - savingsTarget)`

### 7.5 Visual Direction

| Aspect | Specification |
|--------|---------------|
| **Design Language** | Material 3 Expressive |
| **Palette** | Terracotta (#C66B3D), Cream (#F5F0E8), Warm Wood (#8B6B47), Deep Green (#2D5A3D), Charcoal (#33302D) |
| **Motion** | Spring-based (stiffness: medium, damping: medium), overshoot allowed, entrance = doorway walk-through |
| **Typography** | Display: Large/Expressive for safe-to-spend; Body: Humanist (Inter or system) for character copy |
| **Texture** | Soft elevation (2-8dp), rounded corners (16-28dp), paper/canvas background on cards |
| **Dark Mode** | Not primary; warm dark variant if needed later |

---

## 8. Character Logic (Guardrails from Brief §10)

### 8.1 Maa (Motherly Coach)

**Hard Rules (enforced in code):**
1. Silence = default (no insight = approval)
2. Patterns only, never single transactions (≥3× in 7 days minimum)
3. Never "you should have" — only questions and offers
4. Exactly one doable next step per insight
5. Praise is baseline (templates 15-20)
6. Context-aware silence: hard week, post-overspend, streak broken → suppress
7. No invented numbers — all from engine
8. No moralizing language

**Voice Budget:** Max 1/day, 3/week (tracked in DataStore)

### 8.2 Bhai (Elder Brother Wallet)

**Reframe Rules:**
- "We" framing, never "you should"
- Gullak grows visibly in real-time
- Named savings goal from onboarding = destination
- Gap = winnable challenge (distance closable)
- Slips absorbed quietly, gap widens slightly
- Gain framing: "Bhai's Gullak is at ₹80,000, and it's yours" (never "you lost ₹80,000")
- Reinvest illustration: illustrative only, clear disclaimer

---

## 9. Onboarding Flow (8 Steps)

| Step | Question | Calibrates |
|------|----------|------------|
| 1 | Age band | Tone register, typical categories |
| 2 | Life stage (student/salaried/self-employed/other) | Budget archetype |
| 3 | Rough monthly income | Denominator for safe-to-spend |
| 4 | Fixed vs flexible (rent, EMIs, etc.) | Discretionary definition |
| 5 | Habit type (food delivery, cabs, shopping, subs, chai, nights out) | Maa's watch patterns |
| 6 | Mentality MCQ (impulse vs planned) | **GuiltSensitivity** (key!) |
| 7 | "What are you saving for?" | Bhai's Gullak destination |
| 8 | Pain dial (optional) | Animation intensity |

**Completion:** Sets `onboarding_completed = true`, navigates to `house` route → entry animation plays.

---

## 10. Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.0+ |
| UI | Jetpack Compose (Material 3 Expressive) |
| Navigation | Compose Navigation |
| Architecture | MVVM (ViewModel + immutable UI State) |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Preferences | DataStore (Preferences Proto) |
| Async | Coroutines + Flow |
| Capture | NotificationListenerService + SMS BroadcastReceiver |
| Animations | Compose Animation APIs (spring, AnimatedContent, Canvas) |
| Build | Gradle Kotlin DSL, AGP 8.5+ |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |

---

## 11. Permissions Required

```xml
<!-- Manifest -->
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> <!-- for insight notifications if added -->
```

**Note:** NotificationListenerService requires user to manually enable in Settings → Notification Access. App guides user there with intent.

---

## 12. Testing Strategy

| Layer | Approach |
|-------|----------|
| **Core Engine** | Pure Kotlin JUnit tests (JVM) — 100% coverage target for calculator, pattern detector, insight engine |
| **Repositories** | Room in-memory database tests |
| **ViewModels** | Turbine / kotlinx-coroutines-test for StateFlow testing |
| **UI** | Compose UI tests (Screenshot testing for visual regression) |
| **Capture** | Unit tests for parsers with real notification/SMS samples |
| **Integration** | Hilt test modules, full flow tests for onboarding → house → kitchen → bhairoom |

---

## 13. Build & Delivery

### 13.1 Local Build
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 13.2 GitHub Actions CI (for APK distribution)
```yaml
# .github/workflows/build-apk.yml
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21' }
      - name: Build Debug APK
        run: ./gradlew assembleDebug
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with: { name: ghar-debug-apk, path: app/build/outputs/apk/debug/*.apk }
```

### 13.3 GitHub Releases (for user testing)
- Tag release → workflow builds signed APK → attaches to GitHub Release
- User downloads APK from Releases page → installs (sideload)
- Requires: Keystore in GitHub Secrets (or debug keystore for testing)

---

## 14. Guardrails Checklist (Non-Negotiable)

- [ ] **No shame, ever** — No "you should have," no blame, no failure scoreboard
- [ ] **No hallucinated numbers** — Every figure engine-derived
- [ ] **Restart, not reckoning** — Slip-ups get forgiveness + restart
- [ ] **Privacy** — All financial data on-device, no network calls in v1
- [ ] **Not financial advice** — Reinvest illustrations carry disclaimer
- [ ] **Graceful degradation** — Denied permissions → manual capture, no nagging

---

## 15. Out of Scope (Explicitly v2+)

- "Talk to Maa" LLM chat (Firebase AI Logic)
- Household/shared mode
- Wear OS / widget
- Actual instrument connections (SIP/RD/PPF)
- Cloud sync / backup
- Export/import (CSV, etc.)

---

## 16. Implementation Order (Writing-Plans Will Detail)

1. **Project setup** — Gradle, Hilt, Room, DataStore, Compose, Material 3 Expressive
2. **Core engine** — SafeToSpendCalculator, PatternDetector, InsightEngine, InsightTemplates (with JVM tests)
3. **Data layer** — Entities, DAOs, Database, Repositories, DataStore
4. **Capture layer** — NotificationListenerService, SMS parser, dedupe
4. **Theme & common components** — Colors, typography, shapes, base components
5. **Onboarding** — 8-step flow, calibration persistence, entry animation trigger
6. **House + Entry Animation** — Spatial navigation, doorway transition
7. **Kitchen** — Safe-to-spend display, Maa insight card, transaction list, quick-add
8. **BhaiRoom** — Gullak animation, goal progress, reinvest illustration
9. **Physical Cash Animation** — Signature interaction, haptics, sound
10. **Integration & Polish** — End-to-end flow, edge cases, guardrails verification
11. **CI/CD** — GitHub Actions for APK build + Release automation

---

*End of Design Specification. Ready for implementation planning via `writing-plans` skill.*