package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.AppContainer
import com.example.core.model.Transaction
import com.example.data.local.LocalVaultSnapshot
import com.example.data.preferences.DailyReminderRoutine
import com.example.ui.widget.GharQuickStampWidgetUpdater
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class VaultExportSummary(
  val transactionCount: Int,
  val goalCount: Int,
  val stampCount: Int,
  val fileNameHint: String
)

data class ParsedVaultBackup(
  val snapshot: LocalVaultSnapshot,
  val exportedAtMs: Long,
  val plainEnglishMode: Boolean?,
  val themeMode: String?,
  val useDynamicColor: Boolean?,
  val soundEnabled: Boolean?,
  val hapticsEnabled: Boolean?,
  val smartRandomNudgesEnabled: Boolean?,
  val autoBalanceAlertsEnabled: Boolean?,
  val dailyRoutines: List<DailyReminderRoutine>?
) {
  val userNameDisplay: String
    get() = snapshot.household.userName.trim().ifBlank { "Friend" }

  val transactionCount: Int
    get() = snapshot.transactions.size

  val goalCount: Int
    get() = snapshot.goals.size

  val stampCount: Int
    get() = snapshot.quickChips.size
}

object VaultBackupManager {

  private const val BACKUP_FORMAT_ID = "ghar_portable_vault"
  private const val BACKUP_SCHEMA_VERSION = 2

  fun suggestedJsonBackupFileName(nowMs: Long = System.currentTimeMillis()): String {
    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
    return "ghar_backup_$stamp.json"
  }

  fun suggestedCsvExportFileName(nowMs: Long = System.currentTimeMillis()): String {
    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMs))
    return "ghar_khata_$stamp.csv"
  }

  fun buildBackupJsonString(container: AppContainer, nowMs: Long = System.currentTimeMillis()): String {
    val prefs = container.preferencesRepository.prefsSnapshot.value
    val vaultObj = container.exportVaultJsonObject()

    val prefsObj = JSONObject().apply {
      put("plainEnglishMode", prefs.plainEnglishMode)
      put("themeMode", prefs.themeMode)
      put("useDynamicColor", prefs.useDynamicColor)
      put("soundEnabled", prefs.soundEnabled)
      put("hapticsEnabled", prefs.hapticsEnabled)
      put("smartRandomNudgesEnabled", prefs.smartRandomNudgesEnabled)
      put("autoBalanceAlertsEnabled", prefs.autoBalanceAlertsEnabled)
      put("dailyRoutinesEncoded", DailyReminderRoutine.encodeList(prefs.dailyRoutines))
    }

    val envelope = JSONObject().apply {
      put("format", BACKUP_FORMAT_ID)
      put("schemaVersion", BACKUP_SCHEMA_VERSION)
      put("app", "Ghar")
      put("exportedAtMs", nowMs)
      put("vault", vaultObj)
      put("preferences", prefsObj)
    }

    return envelope.toString(2)
  }

  fun parseBackupJsonString(rawJson: String, container: AppContainer): Result<ParsedVaultBackup> {
    return runCatching {
      val trimmed = rawJson.trim().removePrefix("\uFEFF")
      require(trimmed.startsWith("{")) { "Selected file is not a valid Ghar JSON backup." }

      val root = JSONObject(trimmed)
      val vaultObj = when {
        root.optJSONObject("vault") != null -> root.getJSONObject("vault")
        root.has("household") || root.has("transactions") || root.has("goals") || root.has("gullakHonestV2") -> root
        else -> throw IllegalArgumentException("Unrecognized JSON structure. Please select a Ghar .json backup file.")
      }

      val parsedSnapshot = container.parseVaultJsonObject(vaultObj)
      val prefsObj = root.optJSONObject("preferences")

      val routines = prefsObj?.optString("dailyRoutinesEncoded", "")
        ?.takeIf { it.isNotBlank() }
        ?.let { DailyReminderRoutine.decodeList(it) }

      ParsedVaultBackup(
        snapshot = parsedSnapshot,
        exportedAtMs = root.optLong("exportedAtMs", 0L),
        plainEnglishMode = if (prefsObj?.has("plainEnglishMode") == true) prefsObj.optBoolean("plainEnglishMode") else null,
        themeMode = prefsObj?.optString("themeMode")?.takeIf { it.isNotBlank() },
        useDynamicColor = if (prefsObj?.has("useDynamicColor") == true) prefsObj.optBoolean("useDynamicColor") else null,
        soundEnabled = if (prefsObj?.has("soundEnabled") == true) prefsObj.optBoolean("soundEnabled") else null,
        hapticsEnabled = if (prefsObj?.has("hapticsEnabled") == true) prefsObj.optBoolean("hapticsEnabled") else null,
        smartRandomNudgesEnabled = if (prefsObj?.has("smartRandomNudgesEnabled") == true) {
          prefsObj.optBoolean("smartRandomNudgesEnabled")
        } else {
          null
        },
        autoBalanceAlertsEnabled = if (prefsObj?.has("autoBalanceAlertsEnabled") == true) {
          prefsObj.optBoolean("autoBalanceAlertsEnabled")
        } else {
          null
        },
        dailyRoutines = routines
      )
    }
  }

  suspend fun writeBackupJsonToUri(
    context: Context,
    uri: Uri,
    container: AppContainer
  ): Result<VaultExportSummary> = withContext(Dispatchers.IO) {
    runCatching {
      val jsonString = buildBackupJsonString(container)
      val output = context.contentResolver.openOutputStream(uri, "wt")
        ?: context.contentResolver.openOutputStream(uri)
        ?: error("Unable to open selected destination for writing.")
      output.use { stream ->
        stream.write(jsonString.toByteArray(Charsets.UTF_8))
        stream.flush()
      }

      val txCount = container.transactionRepository.transactionsState.value.size
      val goalCount = container.goalRepository.allGoalsState.value.size
      val stampCount = container.quickChipRepository.quickChipsState.value.size
      VaultExportSummary(
        transactionCount = txCount,
        goalCount = goalCount,
        stampCount = stampCount,
        fileNameHint = suggestedJsonBackupFileName()
      )
    }
  }

  suspend fun readAndValidateBackupFromUri(
    context: Context,
    uri: Uri,
    container: AppContainer
  ): Result<ParsedVaultBackup> = withContext(Dispatchers.IO) {
    runCatching {
      val input = context.contentResolver.openInputStream(uri)
        ?: error("Unable to read the selected backup file.")
      val rawText = input.use { stream ->
        // Guard against accidentally selecting huge non-JSON media files (> 15 MB)
        val bytes = stream.readBytes()
        require(bytes.size <= 15 * 1024 * 1024) { "File is too large to be a Ghar backup." }
        bytes.toString(Charsets.UTF_8)
      }
      parseBackupJsonString(rawText, container).getOrThrow()
    }
  }

  suspend fun applyValidatedBackup(
    context: Context,
    parsed: ParsedVaultBackup,
    container: AppContainer
  ): Result<ParsedVaultBackup> = withContext(Dispatchers.IO) {
    runCatching {
      container.restoreVaultSnapshot(parsed.snapshot)
      val hh = parsed.snapshot.household
      container.preferencesRepository.applyRestoredBackupPreferences(
        userName = hh.userName,
        onboardingCompleted = true,
        plainEnglishMode = parsed.plainEnglishMode,
        themeMode = parsed.themeMode,
        useDynamicColor = parsed.useDynamicColor,
        soundEnabled = parsed.soundEnabled,
        hapticsEnabled = parsed.hapticsEnabled,
        smartRandomNudgesEnabled = parsed.smartRandomNudgesEnabled,
        autoBalanceAlertsEnabled = parsed.autoBalanceAlertsEnabled,
        guiltSensitivity = hh.profile.guiltSensitivity,
        painDial = hh.profile.painDial,
        habitLeaks = hh.habitLeaks.toSet(),
        dailyRoutines = parsed.dailyRoutines
      )
      GharQuickStampWidgetUpdater.enqueueUpdate(context)
      parsed
    }
  }

  fun buildTransactionsCsvString(
    transactions: List<Transaction>,
    plainEnglish: Boolean = false
  ): String {
    val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    val sb = StringBuilder()

    // UTF-8 BOM so Microsoft Excel, Google Sheets, and Apple Numbers detect UTF-8 cleanly
    sb.append('\uFEFF')
    sb.appendLine("Date,Time,Type,Amount_INR,Amount_Paise,Merchant_Or_Note,Category,Recurring,Capture_Source,Transaction_ID")

    val sorted = transactions.sortedByDescending { it.timestamp }
    for (tx in sorted) {
      val dt = Date(tx.timestamp)
      val dateStr = dateFmt.format(dt)
      val timeStr = timeFmt.format(dt)
      val typeStr = if (tx.isExpense) "DEBIT" else "CREDIT"
      val rupeesDecimal = String.format(Locale.US, "%.2f", tx.amountPaise / 100.0)
      val merchantClean = tx.displayMerchant(plainEnglish)
      val categoryClean = tx.category.displayLabel(plainEnglish)
      val recurringStr = if (tx.isRecurring) "Yes" else "No"

      sb.append(escapeCsvField(dateStr)).append(',')
        .append(escapeCsvField(timeStr)).append(',')
        .append(escapeCsvField(typeStr)).append(',')
        .append(rupeesDecimal).append(',')
        .append(tx.amountPaise).append(',')
        .append(escapeCsvField(merchantClean)).append(',')
        .append(escapeCsvField(categoryClean)).append(',')
        .append(recurringStr).append(',')
        .append(escapeCsvField(tx.source.name)).append(',')
        .append(escapeCsvField(tx.id))
        .append('\n')
    }
    return sb.toString()
  }

  suspend fun exportTransactionsCsvToUri(
    context: Context,
    uri: Uri,
    transactions: List<Transaction>,
    plainEnglish: Boolean
  ): Result<Int> = withContext(Dispatchers.IO) {
    runCatching {
      val csvText = buildTransactionsCsvString(transactions, plainEnglish)
      val output = context.contentResolver.openOutputStream(uri, "wt")
        ?: context.contentResolver.openOutputStream(uri)
        ?: error("Unable to open selected CSV file for writing.")
      output.use { stream ->
        stream.write(csvText.toByteArray(Charsets.UTF_8))
        stream.flush()
      }
      transactions.size
    }
  }

  private fun escapeCsvField(raw: String): String {
    val sanitized = raw.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ')
    val needsQuotes = sanitized.contains(',') || sanitized.contains('"')
    return if (needsQuotes) {
      "\"${sanitized.replace("\"", "\"\"")}\""
    } else {
      sanitized
    }
  }
}
