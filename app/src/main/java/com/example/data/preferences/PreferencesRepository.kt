package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.GuiltSensitivity
import com.example.core.model.MaaAiEngineMode
import com.example.core.model.PainDial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ghar_prefs")

enum class RoutineKind {
  MORNING_BRIEF,
  MIDDAY_CHECK,
  EVENING_KHATA,
  CUSTOM;

  companion object {
    fun fromName(raw: String?): RoutineKind =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CUSTOM
  }
}

@Immutable
data class DailyReminderRoutine(
  val id: String,
  val title: String,
  val hour: Int,
  val minute: Int = 0,
  val kind: RoutineKind = RoutineKind.CUSTOM,
  val enabled: Boolean = false
) {
  val safeHour: Int get() = hour.coerceIn(0, 23)
  val safeMinute: Int get() = minute.coerceIn(0, 59)

  fun displayTitle(plainEnglish: Boolean): String {
    if (!plainEnglish) return title
    return when {
      id == "routine_evening" && title.contains("Khata", ignoreCase = true) -> "Evening Expense & Savings Check"
      id == "routine_morning" && title.contains("Rasoi", ignoreCase = true) -> "Morning Daily Budget Brief"
      else -> title
    }
  }

  fun formattedTime(): String = formatRoutineTime(safeHour, safeMinute)

  companion object {
    fun formatRoutineTime(hour24: Int, minute: Int): String {
      val h = hour24.coerceIn(0, 23)
      val m = minute.coerceIn(0, 59)
      val amPm = if (h < 12) "AM" else "PM"
      val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
      }
      return String.format(java.util.Locale.US, "%d:%02d %s", h12, m, amPm)
    }

    fun defaultRoutines(
      eveningEnabled: Boolean = false,
      eveningHour: Int = 21
    ): List<DailyReminderRoutine> = listOf(
      DailyReminderRoutine(
        id = "routine_morning",
        title = "Morning Budget Brief",
        hour = 8,
        minute = 30,
        kind = RoutineKind.MORNING_BRIEF,
        enabled = false
      ),
      DailyReminderRoutine(
        id = "routine_midday",
        title = "Afternoon Spend Check",
        hour = 14,
        minute = 0,
        kind = RoutineKind.MIDDAY_CHECK,
        enabled = false
      ),
      DailyReminderRoutine(
        id = "routine_evening",
        title = "Nightly Khata & Gullak Routine",
        hour = eveningHour.coerceIn(0, 23),
        minute = 0,
        kind = RoutineKind.EVENING_KHATA,
        enabled = eveningEnabled
      )
    )

    fun encodeList(routines: List<DailyReminderRoutine>): String {
      return routines.joinToString("\n") { r ->
        val cleanId = r.id.replace("|", "").replace("\n", "").trim().ifBlank { "routine_custom" }
        val cleanTitle = r.title.replace("|", "/").replace("\n", " ").trim().ifBlank { "Daily Reminder" }
        "$cleanId|$cleanTitle|${r.safeHour}|${r.safeMinute}|${r.kind.name}|${if (r.enabled) "1" else "0"}"
      }
    }

    fun decodeList(
      raw: String?,
      fallbackEveningEnabled: Boolean = false,
      fallbackEveningHour: Int = 21
    ): List<DailyReminderRoutine> {
      if (raw.isNullOrBlank()) {
        return defaultRoutines(fallbackEveningEnabled, fallbackEveningHour)
      }
      val parsed = raw.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
          val parts = line.split("|")
          if (parts.size < 6) return@mapNotNull null
          val id = parts[0].trim().ifBlank { return@mapNotNull null }
          val title = parts[1].trim().ifBlank { "Daily Reminder" }
          val hour = parts[2].toIntOrNull()?.coerceIn(0, 23) ?: 21
          val minute = parts[3].toIntOrNull()?.coerceIn(0, 59) ?: 0
          val kind = RoutineKind.fromName(parts[4])
          val enabled = parts[5] == "1" || parts[5].equals("true", ignoreCase = true)
          DailyReminderRoutine(
            id = id,
            title = title,
            hour = hour,
            minute = minute,
            kind = kind,
            enabled = enabled
          )
        }
        .toList()
      return parsed.ifEmpty { defaultRoutines(fallbackEveningEnabled, fallbackEveningHour) }
    }
  }
}

@Immutable
data class ActiveHabitSankalp(
  val insightId: String,
  val title: String,
  val prepChecklist: String,
  val targetCategoryName: String?,
  val estimatedSavedRupees: Long,
  val reminderHour: Int,
  val reminderMinute: Int,
  val createdAtMs: Long,
  val expiresAtMs: Long
) {
  val formattedTime: String
    get() = DailyReminderRoutine.formatRoutineTime(reminderHour, reminderMinute)

  companion object {
    fun encode(sankalp: ActiveHabitSankalp?): String {
      if (sankalp == null) return ""
      val cat = sankalp.targetCategoryName.orEmpty().replace("|", "")
      return "${sankalp.insightId.replace("|", "")}|${sankalp.title.replace("|", "")}|${sankalp.prepChecklist.replace("|", "")}|$cat|${sankalp.estimatedSavedRupees}|${sankalp.reminderHour}|${sankalp.reminderMinute}|${sankalp.createdAtMs}|${sankalp.expiresAtMs}"
    }

    fun decode(raw: String?): ActiveHabitSankalp? {
      if (raw.isNullOrBlank()) return null
      val parts = raw.split("|")
      if (parts.size < 9) return null
      val insightId = parts[0].ifBlank { return null }
      val title = parts[1].ifBlank { "Daily Habit Sankalp" }
      val prep = parts[2]
      val cat = parts[3].ifBlank { null }
      val saved = parts[4].toLongOrNull()?.coerceAtLeast(20L) ?: 50L
      val hour = parts[5].toIntOrNull()?.coerceIn(0, 23) ?: 8
      val minute = parts[6].toIntOrNull()?.coerceIn(0, 59) ?: 30
      val created = parts[7].toLongOrNull() ?: System.currentTimeMillis()
      val expires = parts[8].toLongOrNull() ?: (created + 86_400_000L)
      return ActiveHabitSankalp(
        insightId = insightId,
        title = title,
        prepChecklist = prep,
        targetCategoryName = cat,
        estimatedSavedRupees = saved,
        reminderHour = hour,
        reminderMinute = minute,
        createdAtMs = created,
        expiresAtMs = expires
      )
    }
  }
}

@Immutable
data class AppPreferencesSnapshot(
  val isLoaded: Boolean = false,
  val userName: String = "",
  val onboardingCompleted: Boolean = false,
  val guestModeChosen: Boolean = false,
  val themeMode: String = "SYSTEM",
  val useDynamicColor: Boolean = false,
  val soundEnabled: Boolean = true,
  val hapticsEnabled: Boolean = true,
  val plainEnglishMode: Boolean = false,
  val dailyReminderEnabled: Boolean = false,
  val dailyReminderHour: Int = 21,
  val dailyRoutines: List<DailyReminderRoutine> = DailyReminderRoutine.defaultRoutines(),
  val activeHabitSankalp: ActiveHabitSankalp? = null,
  val smartRandomNudgesEnabled: Boolean = true,
  val lastSmartNudgeAtMs: Long = 0L,
  val lastSmartNudgeKey: String = "",
  val autoBalanceAlertsEnabled: Boolean = true,
  val lastDetectedBankBalancePaise: Long? = null,
  val maaAiEngineMode: MaaAiEngineMode = MaaAiEngineMode.CLOUD_AUTO,
  val quickChipsSeededUsers: Set<String> = emptySet(),
  val seenRoomSplashes: Set<String> = emptySet(),
  val appLockEnabled: Boolean = false
) {
  val isHapticsEnabled: Boolean
    get() = hapticsEnabled

  val activeRoutineCount: Int
    get() = dailyRoutines.count { it.enabled }
}

@Stable
class PreferencesRepository(private val context: Context) {

  private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private object Keys {
    val USER_NAME = stringPreferencesKey("user_name")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val GUILT_SENSITIVITY = stringPreferencesKey("guilt_sensitivity")
    val PAIN_DIAL = stringPreferencesKey("pain_dial")
    val THEME_MODE = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK
    val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    val HABIT_LEAKS = stringSetPreferencesKey("habit_leaks")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    val PLAIN_ENGLISH_MODE = booleanPreferencesKey("plain_english_mode")
    val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
    val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
    val DAILY_ROUTINES_RAW = stringPreferencesKey("daily_routines_raw")
    val ACTIVE_HABIT_SANKALP_RAW = stringPreferencesKey("active_habit_sankalp_raw")
    val SMART_RANDOM_NUDGES_ENABLED = booleanPreferencesKey("smart_random_nudges_enabled")
    val LAST_SMART_NUDGE_AT_MS = longPreferencesKey("last_smart_nudge_at_ms")
    val LAST_SMART_NUDGE_KEY = stringPreferencesKey("last_smart_nudge_key")
    val AUTO_BALANCE_ALERTS_ENABLED = booleanPreferencesKey("auto_balance_alerts_enabled")
    val LAST_DETECTED_BANK_BALANCE_PAISE = longPreferencesKey("last_detected_bank_balance_paise")
    val LAST_INSIGHT_TIMESTAMP = longPreferencesKey("last_insight_timestamp")
    val WEEKLY_INSIGHT_COUNT = intPreferencesKey("weekly_insight_count")
    val LAST_WEEKLY_RESET = longPreferencesKey("last_weekly_reset")
    val MAA_AI_ENGINE_MODE = stringPreferencesKey("maa_ai_engine_mode")
    val QUICK_CHIPS_SEEDED_USERS = stringSetPreferencesKey("quick_chips_seeded_users")
    val GUEST_MODE_CHOSEN = booleanPreferencesKey("guest_mode_chosen")
    val SEEN_ROOM_SPLASHES = stringSetPreferencesKey("seen_room_splashes")
    val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
  }

  /**
   * Single hot StateFlow snapshot shared across the entire app so room switches never
   * instantiate multiple cold DataStore `.map` pipelines or emit staggered recomposition waves.
   */
  val prefsSnapshot: StateFlow<AppPreferencesSnapshot> = context.dataStore.data
    .map { prefs ->
      val fallbackEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false
      val fallbackHour = prefs[Keys.DAILY_REMINDER_HOUR] ?: 21
      val decodedRoutines = DailyReminderRoutine.decodeList(
        raw = prefs[Keys.DAILY_ROUTINES_RAW],
        fallbackEveningEnabled = fallbackEnabled,
        fallbackEveningHour = fallbackHour
      )
      AppPreferencesSnapshot(
        isLoaded = true,
        userName = prefs[Keys.USER_NAME] ?: "",
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        guestModeChosen = prefs[Keys.GUEST_MODE_CHOSEN] ?: false,
        themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
        useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: false,
        soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
        hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
        plainEnglishMode = prefs[Keys.PLAIN_ENGLISH_MODE] ?: false,
        dailyReminderEnabled = decodedRoutines.any { it.enabled } || fallbackEnabled,
        dailyReminderHour = decodedRoutines.firstOrNull { it.id == "routine_evening" }?.hour ?: fallbackHour,
        dailyRoutines = decodedRoutines,
        activeHabitSankalp = ActiveHabitSankalp.decode(prefs[Keys.ACTIVE_HABIT_SANKALP_RAW]),
        smartRandomNudgesEnabled = prefs[Keys.SMART_RANDOM_NUDGES_ENABLED] ?: true,
        lastSmartNudgeAtMs = prefs[Keys.LAST_SMART_NUDGE_AT_MS] ?: 0L,
        lastSmartNudgeKey = prefs[Keys.LAST_SMART_NUDGE_KEY] ?: "",
        autoBalanceAlertsEnabled = prefs[Keys.AUTO_BALANCE_ALERTS_ENABLED] ?: true,
        lastDetectedBankBalancePaise = prefs[Keys.LAST_DETECTED_BANK_BALANCE_PAISE],
        maaAiEngineMode = MaaAiEngineMode.fromName(prefs[Keys.MAA_AI_ENGINE_MODE]),
        quickChipsSeededUsers = prefs[Keys.QUICK_CHIPS_SEEDED_USERS] ?: emptySet(),
        seenRoomSplashes = prefs[Keys.SEEN_ROOM_SPLASHES] ?: emptySet(),
        appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false
      )
    }
    .stateIn(repoScope, SharingStarted.Eagerly, AppPreferencesSnapshot())

  val seenRoomSplashes: Flow<Set<String>> = context.dataStore.data.map { prefs ->
    prefs[Keys.SEEN_ROOM_SPLASHES] ?: emptySet()
  }

  suspend fun markRoomSplashSeen(roomKey: String) {
    if (roomKey.isBlank()) return
    context.dataStore.edit { prefs ->
      val current = prefs[Keys.SEEN_ROOM_SPLASHES] ?: emptySet()
      prefs[Keys.SEEN_ROOM_SPLASHES] = current + roomKey
    }
  }

  suspend fun resetSeenRoomSplashes() {
    context.dataStore.edit { prefs ->
      prefs[Keys.SEEN_ROOM_SPLASHES] = emptySet()
    }
  }

  val userName: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[Keys.USER_NAME] ?: ""
  }

  val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[Keys.ONBOARDING_COMPLETED] ?: false
  }

  val isGuestModeChosen: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[Keys.GUEST_MODE_CHOSEN] ?: false
  }

  val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[Keys.THEME_MODE] ?: "SYSTEM"
  }

  val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[Keys.USE_DYNAMIC_COLOR] ?: false
  }

  val habitLeaks: Flow<Set<String>> = context.dataStore.data.map { prefs ->
    prefs[Keys.HABIT_LEAKS] ?: setOf("CHAI_SNACKS")
  }

  val guiltSensitivity: Flow<GuiltSensitivity> = context.dataStore.data.map { prefs ->
    val name = prefs[Keys.GUILT_SENSITIVITY]
    GuiltSensitivity.fromName(name)
  }

  val painDial: Flow<PainDial> = context.dataStore.data.map { prefs ->
    val name = prefs[Keys.PAIN_DIAL]
    PainDial.fromName(name)
  }

  val isSoundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[Keys.SOUND_ENABLED] ?: true
  }

  val isHapticsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[Keys.HAPTICS_ENABLED] ?: true
  }

  val lastInsightTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
    prefs[Keys.LAST_INSIGHT_TIMESTAMP] ?: 0L
  }

  val weeklyInsightCount: Flow<Int> = context.dataStore.data.map { prefs ->
    prefs[Keys.WEEKLY_INSIGHT_COUNT] ?: 0
  }

  val maaAiEngineMode: Flow<MaaAiEngineMode> = context.dataStore.data.map { prefs ->
    MaaAiEngineMode.fromName(prefs[Keys.MAA_AI_ENGINE_MODE])
  }

  fun observeQuickChipsSeeded(userId: String): Flow<Boolean> = context.dataStore.data.map { prefs ->
    val seededUsers = prefs[Keys.QUICK_CHIPS_SEEDED_USERS] ?: emptySet()
    userId in seededUsers
  }

  suspend fun isQuickChipsSeeded(userId: String): Boolean {
    val prefs = context.dataStore.data.first()
    return userId in (prefs[Keys.QUICK_CHIPS_SEEDED_USERS] ?: emptySet())
  }

  suspend fun markQuickChipsSeeded(userId: String) {
    if (userId.isBlank()) return
    context.dataStore.edit { prefs ->
      val current = prefs[Keys.QUICK_CHIPS_SEEDED_USERS] ?: emptySet()
      prefs[Keys.QUICK_CHIPS_SEEDED_USERS] = current + userId
    }
  }

  suspend fun setMaaAiEngineMode(mode: MaaAiEngineMode) {
    context.dataStore.edit { prefs ->
      prefs[Keys.MAA_AI_ENGINE_MODE] = mode.name
    }
  }

  suspend fun setUserName(name: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.USER_NAME] = name.trim()
    }
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.ONBOARDING_COMPLETED] = completed
    }
  }

  suspend fun setThemeMode(mode: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = mode
    }
  }

  suspend fun setUseDynamicColor(useDynamic: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.USE_DYNAMIC_COLOR] = useDynamic
    }
  }

  suspend fun setHabitLeaks(leaks: Set<String>) {
    context.dataStore.edit { prefs ->
      prefs[Keys.HABIT_LEAKS] = leaks
    }
  }

  suspend fun setGuiltSensitivity(sensitivity: GuiltSensitivity) {
    context.dataStore.edit { prefs ->
      prefs[Keys.GUILT_SENSITIVITY] = sensitivity.name
    }
  }

  suspend fun setPainDial(painDial: PainDial) {
    context.dataStore.edit { prefs ->
      prefs[Keys.PAIN_DIAL] = painDial.name
    }
  }

  suspend fun setSoundEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.SOUND_ENABLED] = enabled
    }
  }

  suspend fun setHapticsEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.HAPTICS_ENABLED] = enabled
    }
  }

  suspend fun recordInsightShown(timestamp: Long = System.currentTimeMillis()) {
    context.dataStore.edit { prefs ->
      val now = timestamp
      val lastReset = prefs[Keys.LAST_WEEKLY_RESET] ?: 0L
      val isNewWeek = (now - lastReset) > (7 * 24 * 3600 * 1000L)

      val currentCount = if (isNewWeek) 0 else (prefs[Keys.WEEKLY_INSIGHT_COUNT] ?: 0)
      prefs[Keys.WEEKLY_INSIGHT_COUNT] = currentCount + 1
      prefs[Keys.LAST_INSIGHT_TIMESTAMP] = now
      if (isNewWeek) {
        prefs[Keys.LAST_WEEKLY_RESET] = now
      }
    }
  }

  val plainEnglishMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[Keys.PLAIN_ENGLISH_MODE] ?: false
  }

  suspend fun setPlainEnglishMode(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.PLAIN_ENGLISH_MODE] = enabled
    }
  }

  suspend fun setDailyReminder(enabled: Boolean, hourOfDay: Int = 21) {
    val cleanHour = hourOfDay.coerceIn(0, 23)
    context.dataStore.edit { prefs ->
      prefs[Keys.DAILY_REMINDER_ENABLED] = enabled
      prefs[Keys.DAILY_REMINDER_HOUR] = cleanHour
      val current = DailyReminderRoutine.decodeList(
        raw = prefs[Keys.DAILY_ROUTINES_RAW],
        fallbackEveningEnabled = enabled,
        fallbackEveningHour = cleanHour
      )
      val updated = if (current.any { it.id == "routine_evening" }) {
        current.map { r ->
          if (r.id == "routine_evening") r.copy(enabled = enabled, hour = cleanHour) else r
        }
      } else {
        current + DailyReminderRoutine(
          id = "routine_evening",
          title = "Nightly Khata & Gullak Routine",
          hour = cleanHour,
          minute = 0,
          kind = RoutineKind.EVENING_KHATA,
          enabled = enabled
        )
      }
      prefs[Keys.DAILY_ROUTINES_RAW] = DailyReminderRoutine.encodeList(updated)
    }
  }

  suspend fun saveDailyRoutines(routines: List<DailyReminderRoutine>) {
    context.dataStore.edit { prefs ->
      val normalized = routines.sortedWith(compareBy({ it.safeHour }, { it.safeMinute }))
      prefs[Keys.DAILY_ROUTINES_RAW] = DailyReminderRoutine.encodeList(normalized)
      val evening = normalized.firstOrNull { it.id == "routine_evening" }
      prefs[Keys.DAILY_REMINDER_ENABLED] = normalized.any { it.enabled }
      if (evening != null) {
        prefs[Keys.DAILY_REMINDER_HOUR] = evening.safeHour
      }
    }
  }

  suspend fun upsertDailyRoutine(routine: DailyReminderRoutine): List<DailyReminderRoutine> {
    var resultList: List<DailyReminderRoutine> = emptyList()
    context.dataStore.edit { prefs ->
      val current = DailyReminderRoutine.decodeList(
        raw = prefs[Keys.DAILY_ROUTINES_RAW],
        fallbackEveningEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
        fallbackEveningHour = prefs[Keys.DAILY_REMINDER_HOUR] ?: 21
      )
      val updated = if (current.any { it.id == routine.id }) {
        current.map { if (it.id == routine.id) routine else it }
      } else {
        current + routine
      }.sortedWith(compareBy({ it.safeHour }, { it.safeMinute }))
      resultList = updated
      prefs[Keys.DAILY_ROUTINES_RAW] = DailyReminderRoutine.encodeList(updated)
      prefs[Keys.DAILY_REMINDER_ENABLED] = updated.any { it.enabled }
      updated.firstOrNull { it.id == "routine_evening" }?.let {
        prefs[Keys.DAILY_REMINDER_HOUR] = it.safeHour
      }
    }
    return resultList
  }

  suspend fun deleteDailyRoutine(routineId: String): List<DailyReminderRoutine> {
    var resultList: List<DailyReminderRoutine> = emptyList()
    context.dataStore.edit { prefs ->
      val current = DailyReminderRoutine.decodeList(
        raw = prefs[Keys.DAILY_ROUTINES_RAW],
        fallbackEveningEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
        fallbackEveningHour = prefs[Keys.DAILY_REMINDER_HOUR] ?: 21
      )
      val updated = current.filterNot { it.id == routineId }
      resultList = updated
      prefs[Keys.DAILY_ROUTINES_RAW] = DailyReminderRoutine.encodeList(updated)
      prefs[Keys.DAILY_REMINDER_ENABLED] = updated.any { it.enabled }
    }
    return resultList
  }

  suspend fun setSmartRandomNudgesEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.SMART_RANDOM_NUDGES_ENABLED] = enabled
    }
  }

  suspend fun recordSmartNudgeSent(nudgeKey: String, timestampMs: Long = System.currentTimeMillis()) {
    context.dataStore.edit { prefs ->
      prefs[Keys.LAST_SMART_NUDGE_KEY] = nudgeKey
      prefs[Keys.LAST_SMART_NUDGE_AT_MS] = timestampMs
    }
  }

  suspend fun setAutoBalanceAlertsEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.AUTO_BALANCE_ALERTS_ENABLED] = enabled
    }
  }

  suspend fun setLastDetectedBankBalancePaise(balancePaise: Long?) {
    context.dataStore.edit { prefs ->
      if (balancePaise != null && balancePaise >= 0L) {
        prefs[Keys.LAST_DETECTED_BANK_BALANCE_PAISE] = balancePaise
      } else {
        prefs.remove(Keys.LAST_DETECTED_BANK_BALANCE_PAISE)
      }
    }
  }

  suspend fun setGuestModeChosen(chosen: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.GUEST_MODE_CHOSEN] = chosen
    }
  }

  suspend fun setActiveHabitSankalp(sankalp: ActiveHabitSankalp?) {
    context.dataStore.edit { prefs ->
      if (sankalp == null) {
        prefs.remove(Keys.ACTIVE_HABIT_SANKALP_RAW)
      } else {
        prefs[Keys.ACTIVE_HABIT_SANKALP_RAW] = ActiveHabitSankalp.encode(sankalp)
      }
    }
  }

  suspend fun setAppLockEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.APP_LOCK_ENABLED] = enabled
    }
  }

  suspend fun applyRestoredBackupPreferences(
    userName: String?,
    onboardingCompleted: Boolean,
    plainEnglishMode: Boolean?,
    themeMode: String?,
    useDynamicColor: Boolean?,
    soundEnabled: Boolean?,
    hapticsEnabled: Boolean?,
    smartRandomNudgesEnabled: Boolean?,
    autoBalanceAlertsEnabled: Boolean?,
    guiltSensitivity: GuiltSensitivity?,
    painDial: PainDial?,
    habitLeaks: Set<String>?,
    dailyRoutines: List<DailyReminderRoutine>?
  ) {
    context.dataStore.edit { prefs ->
      if (!userName.isNullOrBlank()) {
        prefs[Keys.USER_NAME] = userName.trim()
      }
      prefs[Keys.ONBOARDING_COMPLETED] = onboardingCompleted
      plainEnglishMode?.let { prefs[Keys.PLAIN_ENGLISH_MODE] = it }
      themeMode?.takeIf { it.isNotBlank() }?.let { prefs[Keys.THEME_MODE] = it }
      useDynamicColor?.let { prefs[Keys.USE_DYNAMIC_COLOR] = it }
      soundEnabled?.let { prefs[Keys.SOUND_ENABLED] = it }
      hapticsEnabled?.let { prefs[Keys.HAPTICS_ENABLED] = it }
      smartRandomNudgesEnabled?.let { prefs[Keys.SMART_RANDOM_NUDGES_ENABLED] = it }
      autoBalanceAlertsEnabled?.let { prefs[Keys.AUTO_BALANCE_ALERTS_ENABLED] = it }
      guiltSensitivity?.let { prefs[Keys.GUILT_SENSITIVITY] = it.name }
      painDial?.let { prefs[Keys.PAIN_DIAL] = it.name }
      if (!habitLeaks.isNullOrEmpty()) {
        prefs[Keys.HABIT_LEAKS] = habitLeaks
      }
      if (!dailyRoutines.isNullOrEmpty()) {
        val normalized = dailyRoutines.sortedWith(compareBy({ it.safeHour }, { it.safeMinute }))
        prefs[Keys.DAILY_ROUTINES_RAW] = DailyReminderRoutine.encodeList(normalized)
        prefs[Keys.DAILY_REMINDER_ENABLED] = normalized.any { it.enabled }
        normalized.firstOrNull { it.id == "routine_evening" }?.let {
          prefs[Keys.DAILY_REMINDER_HOUR] = it.safeHour
        }
      }
    }
  }

  suspend fun resetOnboardingForTesting() {
    context.dataStore.edit { prefs ->
      prefs[Keys.ONBOARDING_COMPLETED] = false
      prefs[Keys.GUEST_MODE_CHOSEN] = false
      prefs[Keys.SEEN_ROOM_SPLASHES] = emptySet()
    }
  }
}
