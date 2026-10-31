package com.example.ui.security

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ui.common.PebbleCardShape
import com.example.ui.common.pastelHomeBackdrop
import com.example.ui.theme.PastelHoney
import com.example.ui.theme.PastelPlum
import com.example.ui.theme.PastelSage
import com.example.ui.theme.PastelTerracotta

private const val BACKGROUND_RELOCK_THRESHOLD_MS = 15_000L

data class BiometricLockCapability(
  val isSupportedAndEnrolled: Boolean,
  val allowedAuthenticators: Int,
  val includesDeviceCredential: Boolean,
  val statusMessage: String
)

object GharBiometricLockHelper {

  fun findFragmentActivity(context: Context): FragmentActivity? {
    var ctx: Context? = context
    while (ctx is android.content.ContextWrapper) {
      if (ctx is FragmentActivity) return ctx
      ctx = ctx.baseContext
    }
    return null
  }

  fun queryCapability(context: Context): BiometricLockCapability {
    val manager = BiometricManager.from(context)

    val candidates: List<Pair<Int, Boolean>> = buildList {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        add((BIOMETRIC_STRONG or DEVICE_CREDENTIAL) to true)
        add((BIOMETRIC_WEAK or DEVICE_CREDENTIAL) to true)
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        add((BIOMETRIC_WEAK or DEVICE_CREDENTIAL) to true)
      }
      add(BIOMETRIC_STRONG to false)
      add(BIOMETRIC_WEAK to false)
    }

    var sawNoneEnrolled = false
    for ((authMask, hasDeviceCred) in candidates) {
      val code = runCatching { manager.canAuthenticate(authMask) }
        .getOrDefault(BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED)
      if (code == BiometricManager.BIOMETRIC_SUCCESS) {
        return BiometricLockCapability(
          isSupportedAndEnrolled = true,
          allowedAuthenticators = authMask,
          includesDeviceCredential = hasDeviceCred,
          statusMessage = "Fingerprint, Face Unlock, or Device PIN ready"
        )
      }
      if (code == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
        sawNoneEnrolled = true
      }
    }

    return BiometricLockCapability(
      isSupportedAndEnrolled = false,
      allowedAuthenticators = BIOMETRIC_WEAK,
      includesDeviceCredential = false,
      statusMessage = if (sawNoneEnrolled) {
        "Set up a fingerprint or screen lock in Android Settings first to enable App Lock."
      } else {
        "Biometric or screen lock authentication is not available on this device."
      }
    )
  }

  fun authenticate(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onAuthenticated: () -> Unit,
    onErrorOrCancel: (String?) -> Unit
  ) {
    val capability = queryCapability(activity)
    if (!capability.isSupportedAndEnrolled) {
      onErrorOrCancel(capability.statusMessage)
      return
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
      activity,
      executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)
          onAuthenticated()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
          super.onAuthenticationError(errorCode, errString)
          val isUserCancel = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
            errorCode == BiometricPrompt.ERROR_CANCELED
          onErrorOrCancel(if (isUserCancel) null else errString.toString())
        }
      }
    )

    val builder = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setConfirmationRequired(false)
      .setAllowedAuthenticators(capability.allowedAuthenticators)

    // AndroidX Biometric prohibits calling setNegativeButtonText when DEVICE_CREDENTIAL is included
    if (!capability.includesDeviceCredential) {
      builder.setNegativeButtonText(activity.getString(R.string.confirm_dialog_cancel))
    }

    runCatching {
      prompt.authenticate(builder.build())
    }.onFailure { err ->
      onErrorOrCancel(err.localizedMessage ?: "Could not open biometric prompt.")
    }
  }
}

/**
 * Wraps the app UI with optional Biometric / Device-PIN App Lock protection and Recent Apps privacy shielding.
 */
@Composable
fun GharAppLockGate(
  prefsLoaded: Boolean,
  appLockEnabled: Boolean,
  plainEnglish: Boolean,
  content: @Composable () -> Unit
) {
  val context = LocalContext.current
  val activity = remember(context) { GharBiometricLockHelper.findFragmentActivity(context) }
  val lifecycleOwner = LocalLifecycleOwner.current

  var initialLockEvaluated by rememberSaveable { mutableStateOf(false) }
  var isUnlockedInSession by rememberSaveable { mutableStateOf(!appLockEnabled) }
  var lastStoppedAtMs by rememberSaveable { mutableLongStateOf(0L) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  fun triggerPrompt() {
    val hostActivity = activity ?: return
    errorMessage = null
    GharBiometricLockHelper.authenticate(
      activity = hostActivity,
      title = if (plainEnglish) "Unlock Ghar" else "Open Ghar Kundi",
      subtitle = if (plainEnglish) {
        "Confirm your fingerprint or screen lock to view your household budget"
      } else {
        "Confirm your fingerprint or screen lock to step into Rasoi & Gullak"
      },
      onAuthenticated = {
        isUnlockedInSession = true
        lastStoppedAtMs = 0L
        errorMessage = null
      },
      onErrorOrCancel = { msg ->
        errorMessage = msg
      }
    )
  }

  // Shield sensitive household balances in Android Recent Apps thumbnails when App Lock is enabled
  LaunchedEffect(activity, appLockEnabled) {
    val window = activity?.window ?: return@LaunchedEffect
    if (appLockEnabled) {
      window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
      )
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
  }

  // Evaluate cold-start lock state accurately once DataStore hydrates, without re-locking when
  // the user enables App Lock interactively inside Balcony Settings during an active session.
  LaunchedEffect(prefsLoaded, appLockEnabled) {
    if (!prefsLoaded) return@LaunchedEffect
    if (!initialLockEvaluated) {
      initialLockEvaluated = true
      if (appLockEnabled && GharBiometricLockHelper.queryCapability(context).isSupportedAndEnrolled) {
        isUnlockedInSession = false
        triggerPrompt()
      } else {
        isUnlockedInSession = true
      }
    } else if (!appLockEnabled) {
      isUnlockedInSession = true
      errorMessage = null
    }
  }

  val isLocked = prefsLoaded &&
    appLockEnabled &&
    !isUnlockedInSession &&
    GharBiometricLockHelper.queryCapability(context).isSupportedAndEnrolled

  DisposableEffect(lifecycleOwner, prefsLoaded, appLockEnabled) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_STOP -> {
          if (prefsLoaded && appLockEnabled) {
            lastStoppedAtMs = System.currentTimeMillis()
          }
        }
        Lifecycle.Event.ON_START -> {
          if (prefsLoaded && initialLockEvaluated && appLockEnabled) {
            val now = System.currentTimeMillis()
            val awayDuration = if (lastStoppedAtMs > 0L) now - lastStoppedAtMs else 0L
            if (!isUnlockedInSession || awayDuration >= BACKGROUND_RELOCK_THRESHOLD_MS) {
              if (GharBiometricLockHelper.queryCapability(context).isSupportedAndEnrolled) {
                isUnlockedInSession = false
                triggerPrompt()
              }
            }
          }
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    content()

    if (isLocked) {
      BackHandler(enabled = true) {
        activity?.moveTaskToBack(true)
      }
      GharLockScreenOverlay(
        plainEnglish = plainEnglish,
        errorMessage = errorMessage,
        onUnlockClick = { triggerPrompt() }
      )
    }
  }
}

@Composable
private fun GharLockScreenOverlay(
  plainEnglish: Boolean,
  errorMessage: String?,
  onUnlockClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .pastelHomeBackdrop(
        primaryWash = PastelPlum,
        secondaryWash = PastelHoney,
        tertiaryWash = PastelSage
      )
      .systemBarsPadding()
      .padding(horizontal = 24.dp)
      .testTag("ghar_biometric_lock_overlay"),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = PebbleCardShape,
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 3.dp,
      border = BorderStroke(1.2.dp, PastelTerracotta.copy(alpha = 0.45f)),
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 420.dp)
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(PastelTerracotta.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = stringResource(R.string.ghar_locked_title),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(34.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = if (plainEnglish) "Ghar is Locked" else "Ghar Kundi is Latched",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = if (plainEnglish) {
            "Verify your fingerprint, face, or device PIN to view your household budget and savings."
          } else {
            "Verify your fingerprint, face, or device PIN to step inside your Rasoi & Gullak."
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        if (!errorMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(14.dp))
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = errorMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onUnlockClick,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .testTag("unlock_ghar_button")
        ) {
          Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.ghar_unlock_action),
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
